package com.codecrack.execution.executor;

import com.codecrack.execution.model.*;
import com.codecrack.model.TestCase;
import com.codecrack.model.Verdict;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class JavaExecutor implements Executor {

    private final DockerClient dockerClient;

    private static final String DOCKER_IMAGE = "eclipse-temurin:17-alpine";
    private static final long MEMORY_LIMIT = 256L * 1024 * 1024;
    private static final long CPU_QUOTA = 100000L;
    private static final long CPU_PERIOD = 100000L;

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        String containerId = null;
        try {
            log.info("Starting Java execution for submission {}", request.getSubmissionId());

            List<TestCase> testCases = request.getTestCases();
            log.info("TestCases in executor: {}", testCases == null ? "NULL" : testCases.size());

            if (testCases == null || testCases.isEmpty()) {
                return ExecutionResult.builder()
                        .verdict(Verdict.ACCEPTED)
                        .runtimeMs(0)
                        .build();
            }

            containerId = createSecureContainer();
            dockerClient.startContainerCmd(containerId).exec();
            copyCodeToContainer(containerId, request.getCode());

            // Compile
            CompilationResult compilation = compile(containerId);
            if (!compilation.isSuccess()) {
                return ExecutionResult.builder()
                        .verdict(Verdict.COMPILATION_ERROR)
                        .compilationError(compilation.getError())
                        .build();
            }

            // Run test cases
            List<TestCaseResult> results = new ArrayList<>();
            for (int i = 0; i < testCases.size(); i++) {
                TestCase testCase = testCases.get(i);
                log.info("Running test case {}", i + 1);
                // Copy input file to container
                copyInputToContainer(containerId, testCase.getInput(), i + 1);
                TestCaseResult result = runTestCase(containerId, testCase, i + 1);
                results.add(result);
                log.info("Test case {} result: passed={}", i + 1, result.isPassed());
                if (!result.isPassed()) break;
            }

            Verdict verdict = determineVerdict(results, testCases.size());

            int avgRuntime = results.isEmpty() ? 0 :
                    results.stream().mapToInt(TestCaseResult::getRuntime).sum() / results.size();

            return ExecutionResult.builder()
                    .verdict(verdict)
                    .testCaseResults(results)
                    .runtimeMs(avgRuntime)
                    .build();

        } catch (Exception e) {
            log.error("Execution failed for submission {}", request.getSubmissionId(), e);
            return ExecutionResult.builder()
                    .verdict(Verdict.RUNTIME_ERROR)
                    .errorMessage(e.getMessage())
                    .build();
        } finally {
            cleanupContainer(containerId);
        }
    }

    private String createSecureContainer() {
        CreateContainerResponse container = dockerClient
                .createContainerCmd(DOCKER_IMAGE)
                .withCmd("sh", "-c", "while true; do sleep 1; done")
                .withHostConfig(HostConfig.newHostConfig()
                        .withMemory(MEMORY_LIMIT)
                        .withMemorySwap(MEMORY_LIMIT)
                        .withCpuQuota(CPU_QUOTA)
                        .withCpuPeriod(CPU_PERIOD)
                        .withNetworkMode("none")
                        .withPrivileged(false)
                        .withCapDrop(Capability.ALL)
                        .withPidsLimit(50L)
                )
                .withWorkingDir("/tmp")
                .exec();
        return container.getId();
    }

    private void copyCodeToContainer(String containerId, String code) throws IOException {
        copyFileToContainer(containerId, "Solution.java", code.getBytes());
    }

    private void copyInputToContainer(String containerId, String input, int testNumber) throws IOException {
        copyFileToContainer(containerId, "input" + testNumber + ".txt", input.getBytes());
    }

    private void copyFileToContainer(String containerId, String filename, byte[] content) throws IOException {
        ByteArrayOutputStream tarStream = new ByteArrayOutputStream();
        TarArchiveOutputStream tar = new TarArchiveOutputStream(tarStream);
        TarArchiveEntry entry = new TarArchiveEntry(filename);
        entry.setSize(content.length);
        entry.setMode(420);
        tar.putArchiveEntry(entry);
        tar.write(content);
        tar.closeArchiveEntry();
        tar.close();

        dockerClient.copyArchiveToContainerCmd(containerId)
                .withTarInputStream(new ByteArrayInputStream(tarStream.toByteArray()))
                .withRemotePath("/tmp")
                .exec();
    }

    private CompilationResult compile(String containerId) {
        try {
            ExecCreateCmdResponse exec = dockerClient
                    .execCreateCmd(containerId)
                    .withCmd("javac", "-d", "/tmp", "/tmp/Solution.java")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            StringBuilder error = new StringBuilder();

            dockerClient.execStartCmd(exec.getId())
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (frame.getStreamType() == StreamType.STDERR) {
                                error.append(new String(frame.getPayload()));
                            }
                        }
                    }).awaitCompletion(10, TimeUnit.SECONDS);

            if (!error.isEmpty()) {
                log.info("Compilation error: '{}'", error.toString());
                return new CompilationResult(false, error.toString());
            }
            log.info("Compilation successful");
            return new CompilationResult(true, null);

        } catch (Exception e) {
            return new CompilationResult(false, e.getMessage());
        }
    }

    private TestCaseResult runTestCase(String containerId, TestCase testCase, int testNumber) {
        try {
            long startTime = System.currentTimeMillis();

            // Use file redirect instead of stdin pipe
            ExecCreateCmdResponse exec = dockerClient
                    .execCreateCmd(containerId)
                    .withCmd("sh", "-c", "java -cp /tmp Solution < /tmp/input" + testNumber + ".txt")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            ResultCallback.Adapter<Frame> callback = dockerClient
                    .execStartCmd(exec.getId())
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (frame.getStreamType() == StreamType.STDOUT) {
                                output.append(new String(frame.getPayload()));
                            } else if (frame.getStreamType() == StreamType.STDERR) {
                                error.append(new String(frame.getPayload()));
                            }
                        }
                    });

            boolean finished = callback.awaitCompletion(
                    (long) testCase.getTimeLimit(), TimeUnit.MILLISECONDS);
            long runtime = System.currentTimeMillis() - startTime;

            if (!finished) {
                return TestCaseResult.builder()
                        .testCaseNumber(testNumber)
                        .passed(false)
                        .actualOutput("Time Limit Exceeded")
                        .expectedOutput(testCase.getExpectedOutput())
                        .runtime((int) runtime)
                        .build();
            }

            if (!error.isEmpty()) {
                log.info("Test {} stderr: {}", testNumber, error.toString());
                return TestCaseResult.builder()
                        .testCaseNumber(testNumber)
                        .passed(false)
                        .actualOutput("Runtime Error: " + error)
                        .expectedOutput(testCase.getExpectedOutput())
                        .runtime((int) runtime)
                        .build();
            }

            String actual = output.toString().trim();
            String expected = testCase.getExpectedOutput().trim();
            log.info("Test {}: actual='{}' expected='{}'", testNumber, actual, expected);

            return TestCaseResult.builder()
                    .testCaseNumber(testNumber)
                    .passed(actual.equals(expected))
                    .actualOutput(actual)
                    .expectedOutput(expected)
                    .runtime((int) runtime)
                    .build();

        } catch (Exception e) {
            log.error("Test case {} failed with exception: {}", testNumber, e.getMessage(), e);
            return TestCaseResult.builder()
                    .testCaseNumber(testNumber)
                    .passed(false)
                    .actualOutput("Error: " + e.getMessage())
                    .expectedOutput(testCase.getExpectedOutput())
                    .runtime(0)
                    .build();
        }
    }

    private Verdict determineVerdict(List<TestCaseResult> results, int totalTestCases) {
        if (results.isEmpty()) return Verdict.RUNTIME_ERROR;
        TestCaseResult last = results.get(results.size() - 1);
        if (results.size() < totalTestCases) {
            if (last.getActualOutput().contains("Time Limit")) return Verdict.TIME_LIMIT_EXCEEDED;
            if (last.getActualOutput().contains("Runtime Error")) return Verdict.RUNTIME_ERROR;
            return Verdict.WRONG_ANSWER;
        }
        return results.stream().allMatch(TestCaseResult::isPassed)
                ? Verdict.ACCEPTED : Verdict.WRONG_ANSWER;
    }

    private void cleanupContainer(String containerId) {
        if (containerId != null) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                log.info("Cleaned up container: {}", containerId);
            } catch (Exception e) {
                log.error("Failed to cleanup container: {}", containerId, e);
            }
        }
    }

    @Override public String getDockerImage() { return DOCKER_IMAGE; }
    @Override public String getFileExtension() { return ".java"; }
    @Override public String getCompileCommand() { return "javac -d /tmp Solution.java"; }
    @Override public String getRunCommand() { return "java -cp /tmp Solution"; }

    @Data
    @AllArgsConstructor
    private static class CompilationResult {
        private boolean success;
        private String error;
    }
}
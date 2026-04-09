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
public class CppExecutor implements Executor {

    private final DockerClient dockerClient;

    private static final String DOCKER_IMAGE = "gcc:11";
    private static final long MEMORY_LIMIT = 256L * 1024 * 1024;
    private static final long CPU_QUOTA = 100000L;
    private static final long CPU_PERIOD = 100000L;

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        String containerId = null;
        try {
            log.info("Starting C++ execution for submission {}", request.getSubmissionId());

            containerId = createSecureContainer();
            copyCodeToContainer(containerId, request.getCode());
            dockerClient.startContainerCmd(containerId).exec();

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
            for (int i = 0; i < request.getTestCases().size(); i++) {
                TestCase testCase = request.getTestCases().get(i);
                TestCaseResult result = runTestCase(containerId, testCase, i + 1);
                results.add(result);
                if (!result.isPassed()) break;
            }

            Verdict verdict = determineVerdict(results, request.getTestCases().size());

            int avgRuntime = results.isEmpty() ? 0 :
                    results.stream().mapToInt(TestCaseResult::getRuntime).sum() / results.size();

            return ExecutionResult.builder()
                    .verdict(verdict)
                    .testCaseResults(results)
                    .runtimeMs(avgRuntime)
                    .build();

        } catch (Exception e) {
            log.error("C++ execution failed for submission {}", request.getSubmissionId(), e);
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
                .withHostConfig(HostConfig.newHostConfig()
                        .withMemory(MEMORY_LIMIT)
                        .withMemorySwap(MEMORY_LIMIT)
                        .withCpuQuota(CPU_QUOTA)
                        .withCpuPeriod(CPU_PERIOD)
                        .withNetworkMode("none")
                        .withReadonlyRootfs(true)
                        .withTmpFs(Collections.singletonMap("/tmp", "size=50m,mode=1777"))
                        .withPrivileged(false)
                        .withCapDrop(Capability.ALL)
                        .withPidsLimit(50L)
                )
                .withUser("nobody:nogroup")
                .withWorkingDir("/tmp")
                .exec();
        return container.getId();
    }

    private void copyCodeToContainer(String containerId, String code) throws IOException {
        ByteArrayOutputStream tarStream = new ByteArrayOutputStream();
        TarArchiveOutputStream tar = new TarArchiveOutputStream(tarStream);
        byte[] codeBytes = code.getBytes();
        TarArchiveEntry entry = new TarArchiveEntry("solution.cpp");
        entry.setSize(codeBytes.length);
        entry.setMode(420);
        tar.putArchiveEntry(entry);
        tar.write(codeBytes);
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
                    .withCmd("g++", "-o", "/tmp/solution",
                            "/tmp/solution.cpp", "-O2", "-std=c++17", "-Wall")
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
                    }).awaitCompletion(15, TimeUnit.SECONDS);

            // g++ prints warnings to stderr too — only fail if "error:" present
            if (error.toString().contains("error:")) {
                return new CompilationResult(false, error.toString());
            }
            return new CompilationResult(true, null);

        } catch (Exception e) {
            return new CompilationResult(false, e.getMessage());
        }
    }

    private TestCaseResult runTestCase(String containerId, TestCase testCase, int testNumber) {
        try {
            long startTime = System.currentTimeMillis();

            ExecCreateCmdResponse exec = dockerClient
                    .execCreateCmd(containerId)
                    .withCmd("/tmp/solution")
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            ResultCallback.Adapter<Frame> callback = dockerClient
                    .execStartCmd(exec.getId())
                    .withStdIn(new ByteArrayInputStream(testCase.getInput().getBytes()))
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

            return TestCaseResult.builder()
                    .testCaseNumber(testNumber)
                    .passed(actual.equals(expected))
                    .actualOutput(actual)
                    .expectedOutput(expected)
                    .runtime((int) runtime)
                    .build();

        } catch (Exception e) {
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
    @Override public String getFileExtension() { return ".cpp"; }
    @Override public String getCompileCommand() { return "g++ -o /tmp/solution /tmp/solution.cpp -O2 -std=c++17"; }
    @Override public String getRunCommand() { return "/tmp/solution"; }

    @Data
    @AllArgsConstructor
    private static class CompilationResult {
        private boolean success;
        private String error;
    }
}

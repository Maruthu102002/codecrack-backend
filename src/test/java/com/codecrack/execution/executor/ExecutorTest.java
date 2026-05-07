package com.codecrack.execution.executor;

import com.codecrack.execution.model.ExecutionRequest;
import com.codecrack.execution.model.ExecutionResult;
import com.codecrack.model.Verdict;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutorTest {

    @Mock private DockerClient dockerClient;

    @InjectMocks private PythonExecutor pythonExecutor;
    @InjectMocks private JavaExecutor javaExecutor;
    @InjectMocks private CppExecutor cppExecutor;

    @Test
    void pythonExecutor_GetDockerImage_ReturnsImage() {
        assertEquals("python:3.11-alpine", pythonExecutor.getDockerImage());
    }

    @Test
    void pythonExecutor_GetFileExtension_ReturnsPy() {
        assertEquals(".py", pythonExecutor.getFileExtension());
    }

    @Test
    void pythonExecutor_GetCompileCommand_ReturnsNull() {
        assertNull(pythonExecutor.getCompileCommand());
    }

    @Test
    void pythonExecutor_GetRunCommand_ReturnsCommand() {
        assertNotNull(pythonExecutor.getRunCommand());
    }

    @Test
    void javaExecutor_GetDockerImage_ReturnsImage() {
        assertEquals("eclipse-temurin:17-alpine", javaExecutor.getDockerImage());
    }

    @Test
    void javaExecutor_GetFileExtension_ReturnsJava() {
        assertEquals(".java", javaExecutor.getFileExtension());
    }

    @Test
    void javaExecutor_GetCompileCommand_ReturnsCommand() {
        assertNotNull(javaExecutor.getCompileCommand());
    }

    @Test
    void javaExecutor_GetRunCommand_ReturnsCommand() {
        assertNotNull(javaExecutor.getRunCommand());
    }

    @Test
    void cppExecutor_GetDockerImage_ReturnsImage() {
        assertEquals("gcc:13", cppExecutor.getDockerImage());
    }

    @Test
    void cppExecutor_GetFileExtension_ReturnsCpp() {
        assertEquals(".cpp", cppExecutor.getFileExtension());
    }

    @Test
    void cppExecutor_GetCompileCommand_ReturnsCommand() {
        assertNotNull(cppExecutor.getCompileCommand());
    }

    @Test
    void cppExecutor_GetRunCommand_ReturnsCommand() {
        assertNotNull(cppExecutor.getRunCommand());
    }

    @Test
    void pythonExecutor_Execute_DockerFails_ReturnsRuntimeError() {
        when(dockerClient.createContainerCmd(anyString()))
                .thenThrow(new RuntimeException("Docker unavailable"));

        ExecutionRequest request = new ExecutionRequest(1L, "print('hi')", "PYTHON",
                Collections.emptyList(), 1L, 1L);

        ExecutionResult result = pythonExecutor.execute(request);

        assertEquals(Verdict.RUNTIME_ERROR, result.getVerdict());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void javaExecutor_Execute_NoTestCases_ReturnsAccepted() {
        ExecutionRequest request = new ExecutionRequest(1L, "public class Solution{}", "JAVA",
                Collections.emptyList(), 1L, 1L);

        ExecutionResult result = javaExecutor.execute(request);

        assertNotNull(result);
        assertEquals(Verdict.ACCEPTED, result.getVerdict());
    }

    @Test
    void cppExecutor_Execute_DockerFails_ReturnsRuntimeError() {
        when(dockerClient.createContainerCmd(anyString()))
                .thenThrow(new RuntimeException("Docker unavailable"));

        ExecutionRequest request = new ExecutionRequest(1L, "#include<iostream>", "CPP",
                Collections.emptyList(), 1L, 1L);

        ExecutionResult result = cppExecutor.execute(request);

        assertEquals(Verdict.RUNTIME_ERROR, result.getVerdict());
    }
}
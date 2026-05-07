package com.codecrack.execution;
import com.codecrack.execution.executor.CppExecutor;
import com.codecrack.execution.executor.JavaExecutor;
import com.codecrack.execution.executor.PythonExecutor;
import com.codecrack.execution.model.ExecutionRequest;
import com.codecrack.execution.model.ExecutionResult;
import com.codecrack.model.Verdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerExecutionServiceTest {

    @Mock private JavaExecutor javaExecutor;
    @Mock private PythonExecutor pythonExecutor;
    @Mock private CppExecutor cppExecutor;

    @InjectMocks
    private DockerExecutionService dockerExecutionService;

    private ExecutionResult mockResult(Verdict verdict) {
        return ExecutionResult.builder().verdict(verdict).runtimeMs(100).build();
    }

    @Test
    void execute_JavaLanguage_UsesJavaExecutor() {
        when(javaExecutor.execute(any())).thenReturn(mockResult(Verdict.ACCEPTED));
        ExecutionRequest req = new ExecutionRequest(1L, "code", "JAVA", null, 1L, 1L);
        ExecutionResult result = dockerExecutionService.execute(req);
        assertEquals(Verdict.ACCEPTED, result.getVerdict());
        verify(javaExecutor).execute(any());
    }

    @Test
    void execute_PythonLanguage_UsesPythonExecutor() {
        when(pythonExecutor.execute(any())).thenReturn(mockResult(Verdict.ACCEPTED));
        ExecutionRequest req = new ExecutionRequest(1L, "code", "PYTHON", null, 1L, 1L);
        ExecutionResult result = dockerExecutionService.execute(req);
        assertEquals(Verdict.ACCEPTED, result.getVerdict());
        verify(pythonExecutor).execute(any());
    }

    @Test
    void execute_CppLanguage_UsesCppExecutor() {
        when(cppExecutor.execute(any())).thenReturn(mockResult(Verdict.ACCEPTED));
        ExecutionRequest req = new ExecutionRequest(1L, "code", "CPP", null, 1L, 1L);
        ExecutionResult result = dockerExecutionService.execute(req);
        assertEquals(Verdict.ACCEPTED, result.getVerdict());
        verify(cppExecutor).execute(any());
    }

    @Test
    void execute_CppPlusLanguage_UsesCppExecutor() {
        when(cppExecutor.execute(any())).thenReturn(mockResult(Verdict.WRONG_ANSWER));
        ExecutionRequest req = new ExecutionRequest(1L, "code", "C++", null, 1L, 1L);
        ExecutionResult result = dockerExecutionService.execute(req);
        assertEquals(Verdict.WRONG_ANSWER, result.getVerdict());
    }

    @Test
    void execute_UnsupportedLanguage_ThrowsException() {
        ExecutionRequest req = new ExecutionRequest(1L, "code", "RUBY", null, 1L, 1L);
        assertThrows(IllegalArgumentException.class,
                () -> dockerExecutionService.execute(req));
    }

    @Test
    void execute_LowercaseJava_UsesJavaExecutor() {
        when(javaExecutor.execute(any())).thenReturn(mockResult(Verdict.ACCEPTED));
        ExecutionRequest req = new ExecutionRequest(1L, "code", "java", null, 1L, 1L);
        ExecutionResult result = dockerExecutionService.execute(req);
        assertEquals(Verdict.ACCEPTED, result.getVerdict());
    }
}
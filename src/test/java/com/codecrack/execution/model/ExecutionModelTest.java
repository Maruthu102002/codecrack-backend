package com.codecrack.execution.model;

import com.codecrack.model.TestCase;
import com.codecrack.model.Verdict;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionModelTest {

    @Test
    void executionRequest_Builder_SetsAllFields() {
        TestCase tc = new TestCase();
        ExecutionRequest request = ExecutionRequest.builder()
                .submissionId(1L)
                .code("print('hello')")
                .language("PYTHON")
                .testCases(List.of(tc))
                .userId(2L)
                .problemId(3L)
                .build();

        assertEquals(1L, request.getSubmissionId());
        assertEquals("print('hello')", request.getCode());
        assertEquals("PYTHON", request.getLanguage());
        assertEquals(1, request.getTestCases().size());
        assertEquals(2L, request.getUserId());
        assertEquals(3L, request.getProblemId());
    }

    @Test
    void executionRequest_NoArgs_Constructor_Works() {
        ExecutionRequest request = new ExecutionRequest();
        assertNotNull(request);
        assertNull(request.getCode());
    }

    @Test
    void executionRequest_AllArgs_Constructor_Works() {
        ExecutionRequest request = new ExecutionRequest(1L, "code", "JAVA", null, 1L, 1L);
        assertEquals(1L, request.getSubmissionId());
        assertEquals("JAVA", request.getLanguage());
    }

    @Test
    void executionRequest_Setters_Work() {
        ExecutionRequest request = new ExecutionRequest();
        request.setSubmissionId(5L);
        request.setCode("int main(){}");
        request.setLanguage("CPP");
        assertEquals(5L, request.getSubmissionId());
        assertEquals("CPP", request.getLanguage());
    }

    @Test
    void executionResult_Builder_SetsAllFields() {
        ExecutionResult result = ExecutionResult.builder()
                .verdict(Verdict.ACCEPTED)
                .output("hello")
                .errorMessage(null)
                .runtimeMs(100)
                .memoryKb(256)
                .compilationError(null)
                .build();

        assertEquals(Verdict.ACCEPTED, result.getVerdict());
        assertEquals("hello", result.getOutput());
        assertEquals(100, result.getRuntimeMs());
    }

    @Test
    void executionResult_NoArgs_Constructor_Works() {
        ExecutionResult result = new ExecutionResult();
        assertNotNull(result);
        assertNull(result.getVerdict());
    }

    @Test
    void executionResult_Setters_Work() {
        ExecutionResult result = new ExecutionResult();
        result.setVerdict(Verdict.WRONG_ANSWER);
        result.setRuntimeMs(500);
        result.setErrorMessage("Wrong output");
        assertEquals(Verdict.WRONG_ANSWER, result.getVerdict());
        assertEquals(500, result.getRuntimeMs());
    }

    @Test
    void testCaseResult_Builder_SetsAllFields() {
        TestCaseResult result = TestCaseResult.builder()
                .testCaseNumber(1)
                .passed(true)
                .actualOutput("3")
                .expectedOutput("3")
                .runtime(50)
                .build();

        assertEquals(1, result.getTestCaseNumber());
        assertTrue(result.isPassed());
        assertEquals("3", result.getActualOutput());
        assertEquals(50, result.getRuntime());
    }

    @Test
    void testCaseResult_NoArgs_Constructor_Works() {
        TestCaseResult result = new TestCaseResult();
        assertNotNull(result);
        assertFalse(result.isPassed());
    }

    @Test
    void testCaseResult_Setters_Work() {
        TestCaseResult result = new TestCaseResult();
        result.setTestCaseNumber(2);
        result.setPassed(false);
        result.setActualOutput("wrong");
        result.setExpectedOutput("correct");
        assertEquals(2, result.getTestCaseNumber());
        assertFalse(result.isPassed());
    }
}
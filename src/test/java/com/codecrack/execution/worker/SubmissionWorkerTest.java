package com.codecrack.execution.worker;

import com.codecrack.execution.DockerExecutionService;
import com.codecrack.execution.model.ExecutionResult;
import com.codecrack.model.Submission;
import com.codecrack.model.TestCase;
import com.codecrack.model.Verdict;
import com.codecrack.repository.SubmissionRepository;
import com.codecrack.repository.TestCaseRepository;
import com.codecrack.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionWorkerTest {

    @Mock private DockerExecutionService executionService;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private SubmissionService submissionService;

    @InjectMocks
    private SubmissionWorker submissionWorker;

    private Submission mockSubmission() {
        Submission s = new Submission();
        s.setId(1L);
        s.setUserId(1L);
        s.setProblemId(1L);
        s.setCode("print('hello')");
        s.setLanguage("PYTHON");
        s.setVerdict(Verdict.PENDING);
        return s;
    }

    @Test
    void processSubmission_WithTestCases_ExecutesAndUpdates() {
        Submission sub = mockSubmission();
        TestCase tc = new TestCase();
        tc.setInput("1");
        tc.setExpectedOutput("1");

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenReturn(sub);
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L))
                .thenReturn(List.of(tc));
        when(executionService.execute(any()))
                .thenReturn(ExecutionResult.builder()
                        .verdict(Verdict.ACCEPTED)
                        .runtimeMs(100)
                        .memoryKb(256)
                        .build());

        submissionWorker.processSubmission(1L);

        verify(submissionService).updateVerdict(eq(1L), eq(Verdict.ACCEPTED), eq(100), eq(256), any());
    }

    @Test
    void processSubmission_NoTestCases_AcceptsDirectly() {
        Submission sub = mockSubmission();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenReturn(sub);
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L))
                .thenReturn(Collections.emptyList());

        submissionWorker.processSubmission(1L);

        verify(submissionService).updateVerdict(1L, Verdict.ACCEPTED, 0, 0, null);
    }

    @Test
    void processSubmission_NotFound_ThrowsException() {
        when(submissionRepository.findById(99L)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> submissionWorker.processSubmission(99L));
    }

    @Test
    void processSubmission_ExecutionFails_SetsRuntimeError() {
        Submission sub = mockSubmission();
        TestCase tc = new TestCase();
        tc.setInput("1");
        tc.setExpectedOutput("1");

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenReturn(sub);
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L))
                .thenReturn(List.of(tc));
        when(executionService.execute(any()))
                .thenThrow(new RuntimeException("Docker failed"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> submissionWorker.processSubmission(1L));

        verify(submissionService).updateVerdict(eq(1L), eq(Verdict.RUNTIME_ERROR), eq(0), eq(0), anyString());
    }
}
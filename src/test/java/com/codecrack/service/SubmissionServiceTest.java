package com.codecrack.service;

import com.codecrack.model.Problem;
import com.codecrack.model.Submission;
import com.codecrack.model.Verdict;
import com.codecrack.repository.ProblemRepository;
import com.codecrack.repository.SubmissionRepository;
import com.codecrack.repository.TestCaseRepository;
import com.codecrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private CodeSanitizationService codeSanitizationService;

    @InjectMocks
    private SubmissionService submissionService;

    private Problem mockProblem;

    @BeforeEach
    void setUp() {
        mockProblem = Problem.builder()
                .id(1L)
                .title("Two Sum")
                .slug("two-sum")
                .difficulty("EASY")
                .isActive(true)
                .build();

        lenient().when(codeSanitizationService.sanitizeCode(anyString(), anyString()))
                .thenReturn(new CodeSanitizationService.SanitizationResult(true, null));
    }

    @Test
    void submitCode_ValidSubmission_ReturnsSubmission() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(mockProblem));
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L))
                .thenReturn(new ArrayList<>());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(i -> {
            Submission s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        Submission result = submissionService.submitCode(1L, 1L, "print(1)", "PYTHON");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getProblemId());
        assertEquals("PYTHON", result.getLanguage());
    }

    @Test
    void submitCode_WithTestCases_QueuesMessage() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(mockProblem));

        com.codecrack.model.TestCase tc = com.codecrack.model.TestCase.builder()
                .id(1L)
                .problemId(1L)
                .input("1 2")
                .expectedOutput("3")
                .build();
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L))
                .thenReturn(List.of(tc));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(i -> {
            Submission s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        submissionService.submitCode(1L, 1L, "print(1)", "PYTHON");

        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(Object.class));
    }

    @Test
    void submitCode_InvalidProblem_ThrowsException() {
        when(problemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                submissionService.submitCode(1L, 99L, "print(1)", "PYTHON")
        );
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void getSubmission_ValidId_ReturnsSubmission() {
        Submission mockSubmission = Submission.builder()
                .id(1L)
                .userId(1L)
                .problemId(1L)
                .language("PYTHON")
                .verdict(Verdict.ACCEPTED)
                .build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));

        Submission result = submissionService.getSubmission(1L);

        assertNotNull(result);
        assertEquals(Verdict.ACCEPTED, result.getVerdict());
        assertEquals("PYTHON", result.getLanguage());
    }

    @Test
    void getSubmission_InvalidId_ThrowsException() {
        when(submissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                submissionService.getSubmission(99L)
        );
    }

    @Test
    void updateVerdict_AlreadyProcessed_SkipsUpdate() {
        Submission processed = Submission.builder()
                .id(1L)
                .verdict(Verdict.ACCEPTED)
                .build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(processed));

        submissionService.updateVerdict(1L, Verdict.WRONG_ANSWER, 100, 256, null);

        verify(submissionRepository, never()).save(any());
    }
}
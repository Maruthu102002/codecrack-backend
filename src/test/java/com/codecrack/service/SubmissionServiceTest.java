package com.codecrack.service;

import com.codecrack.model.Problem;
import com.codecrack.model.Submission;
import com.codecrack.model.Verdict;
import com.codecrack.repository.ProblemRepository;
import com.codecrack.repository.SubmissionRepository;
import com.codecrack.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.mockito.ArgumentMatchers.isNull;

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
    private com.codecrack.repository.UserRepository userRepository;

    @Mock
    private com.codecrack.service.RedisService redisService;

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
    }

    @Test
    void submitCode_ValidSubmission_ReturnsSubmission() {
        // Arrange
        when(problemRepository.findById(1L)).thenReturn(Optional.of(mockProblem));
        when(testCaseRepository.findByProblemIdOrderByOrderIndexAsc(1L))
                .thenReturn(new ArrayList<>());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(i -> {
            Submission s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        // Act
        Submission result = submissionService.submitCode(1L, 1L, "print(1)", "PYTHON");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getProblemId());
        assertEquals("PYTHON", result.getLanguage());
    }

    @Test
    void submitCode_WithTestCases_QueuesMessage() {
        // Arrange
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

        // Act
        submissionService.submitCode(1L, 1L, "print(1)", "PYTHON");

        // Assert — RabbitMQ called with 2 args
        verify(rabbitTemplate, times(1)).convertAndSend(isNull(), any(Object.class));    }

    @Test
    void submitCode_InvalidProblem_ThrowsException() {
        // Arrange
        when(problemRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                submissionService.submitCode(1L, 99L, "print(1)", "PYTHON")
        );
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void getSubmission_ValidId_ReturnsSubmission() {
        // Arrange
        Submission mockSubmission = Submission.builder()
                .id(1L)
                .userId(1L)
                .problemId(1L)
                .language("PYTHON")
                .verdict(Verdict.ACCEPTED)
                .build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(mockSubmission));

        // Act
        Submission result = submissionService.getSubmission(1L);

        // Assert
        assertNotNull(result);
        assertEquals(Verdict.ACCEPTED, result.getVerdict());
        assertEquals("PYTHON", result.getLanguage());
    }

    @Test
    void getSubmission_InvalidId_ThrowsException() {
        // Arrange
        when(submissionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                submissionService.getSubmission(99L)
        );
    }
}
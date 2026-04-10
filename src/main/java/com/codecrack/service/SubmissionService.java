package com.codecrack.service;

import com.codecrack.execution.model.ExecutionRequest;
import com.codecrack.model.*;
import com.codecrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final TestCaseRepository testCaseRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedisService redisService;

    @Value("${app.queue.submissions}")
    private String submissionsQueue;

    @Transactional
    public Submission submitCode(Long userId, Long problemId, String code, String language) {
        problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));

        Submission submission = Submission.builder()
                .userId(userId)
                .problemId(problemId)
                .code(code)
                .language(language.toUpperCase())
                .verdict(Verdict.PENDING)
                .build();
        submission = submissionRepository.save(submission);

        List<com.codecrack.model.TestCase> testCases =
                testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId);

        if (testCases.isEmpty()) {
            submission.setVerdict(Verdict.ACCEPTED);
            return submissionRepository.save(submission);
        }

        ExecutionRequest request = ExecutionRequest.builder()
                .submissionId(submission.getId())
                .code(code)
                .language(language.toUpperCase())
                .testCases(testCases)
                .userId(userId)
                .problemId(problemId)
                .build();

        rabbitTemplate.convertAndSend(submissionsQueue, request);
        log.info("Queued submission {} for execution", submission.getId());

        return submission;
    }

    @Transactional
    public void updateVerdict(Long submissionId, Verdict verdict,
                              int runtimeMs, int memoryKb, String errorMessage) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        submission.setVerdict(verdict);
        submission.setRuntimeMs(runtimeMs);
        submission.setMemoryKb(memoryKb);
        submission.setErrorMessage(errorMessage);
        submission.setCompletedAt(java.time.LocalDateTime.now());
        submissionRepository.save(submission);

        if (verdict == Verdict.ACCEPTED) {
            updateUserStats(submission.getUserId(), true);
        } else {
            updateUserStats(submission.getUserId(), false);
        }

        // Update leaderboard after stats updated
        userRepository.findById(submission.getUserId()).ifPresent(u -> {
            redisService.updateLeaderboard(
                    u.getId().toString(),
                    u.getProblemsSolved(),
                    u.getTotalSubmissions()
            );
        });

        log.info("Updated submission {} verdict: {}", submissionId, verdict);
    }

    private void updateUserStats(Long userId, boolean accepted) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setTotalSubmissions(user.getTotalSubmissions() + 1);
            if (accepted) {
                user.setAcceptedSubmissions(user.getAcceptedSubmissions() + 1);
                user.setProblemsSolved(user.getProblemsSolved() + 1);
            }
            userRepository.save(user);
        });
    }

    public Submission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + id));
    }

    public List<Submission> getUserSubmissions(Long userId) {
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
    }
}
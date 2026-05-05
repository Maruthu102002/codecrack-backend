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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    private final CodeSanitizationService codeSanitizationService; // Fix 1: Wire sanitization

    @Value("${app.queue.submissions}")
    private String submissionsQueue;

    @Transactional
    public Submission submitCode(Long userId, Long problemId, String code, String language) {

        // Fix 1: Sanitize code before processing
        CodeSanitizationService.SanitizationResult sanitization =
                codeSanitizationService.sanitizeCode(code, language);
        if (!sanitization.isValid()) {
            throw new RuntimeException("Code rejected: " + sanitization.getReason());
        }

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

        rabbitTemplate.convertAndSend(submissionsQueue, submission.getId());
        log.info("Queued submission {} for execution", submission.getId());

        return submission;
    }

    @Transactional
    public void updateVerdict(Long submissionId, Verdict verdict,
                              int runtimeMs, int memoryKb, String errorMessage) {

        // Fix 3: Idempotency — skip if already processed
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        if (submission.getVerdict() != Verdict.PENDING) {
            log.warn("Submission {} already processed with verdict {}. Skipping.",
                    submissionId, submission.getVerdict());
            return;
        }

        submission.setVerdict(verdict);
        submission.setRuntimeMs(runtimeMs);
        submission.setMemoryKb(memoryKb);
        submission.setErrorMessage(errorMessage);
        submission.setCompletedAt(java.time.LocalDateTime.now());
        submissionRepository.save(submission);

        if (verdict == Verdict.ACCEPTED) {
            updateUserStats(submission.getUserId(), submission.getProblemId(), true);
        } else {
            updateUserStats(submission.getUserId(), submission.getProblemId(), false);
        }

        userRepository.findById(submission.getUserId()).ifPresent(u -> {
            redisService.updateLeaderboard(
                    u.getId().toString(),
                    u.getProblemsSolved(),
                    u.getTotalSubmissions()
            );
        });

        log.info("Updated submission {} verdict: {}", submissionId, verdict);
    }

    private void updateUserStats(Long userId, Long problemId, boolean accepted) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setTotalSubmissions(user.getTotalSubmissions() + 1);
            if (accepted) {
                user.setAcceptedSubmissions(user.getAcceptedSubmissions() + 1);

                // Fix 2: Duplicate solve check — only increment if not solved before
                boolean alreadySolved = submissionRepository
                        .existsByUserIdAndProblemIdAndVerdict(userId, problemId, Verdict.ACCEPTED);
                if (!alreadySolved) {
                    user.setProblemsSolved(user.getProblemsSolved() + 1);
                }
            }
            userRepository.save(user);
        });
    }

    public Submission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + id));
    }

    public Page<Submission> getUserSubmissions(Long userId, Pageable pageable) {
        return submissionRepository.findByUserId(userId, pageable);
    }
}
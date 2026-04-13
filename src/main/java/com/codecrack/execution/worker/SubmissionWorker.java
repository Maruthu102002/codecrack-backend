package com.codecrack.execution.worker;

import com.codecrack.execution.DockerExecutionService;
import com.codecrack.execution.model.ExecutionRequest;
import com.codecrack.execution.model.ExecutionResult;
import com.codecrack.model.Submission;
import com.codecrack.model.TestCase;
import com.codecrack.model.Verdict;
import com.codecrack.repository.SubmissionRepository;
import com.codecrack.repository.TestCaseRepository;
import com.codecrack.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionWorker {

    private final DockerExecutionService executionService;
    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionService submissionService;

    @RabbitListener(queues = "${app.queue.submissions}")
    public void processSubmission(Long submissionId) {
        log.info("Worker picked submission: {}", submissionId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        try {
            submission.setVerdict(Verdict.RUNNING);
            submissionRepository.save(submission);

            List<TestCase> testCases = testCaseRepository
                    .findByProblemIdOrderByOrderIndexAsc(submission.getProblemId());

            log.info("Loaded {} test cases for problem {}", testCases.size(), submission.getProblemId());

            if (testCases.isEmpty()) {
                log.warn("No test cases found for problem {}", submission.getProblemId());
                submissionService.updateVerdict(submissionId, Verdict.ACCEPTED, 0, 0, null);
                return;
            }

            ExecutionRequest request = ExecutionRequest.builder()
                    .submissionId(submissionId)
                    .code(submission.getCode())
                    .language(submission.getLanguage())
                    .testCases(testCases)
                    .userId(submission.getUserId())
                    .problemId(submission.getProblemId())
                    .build();

            log.info("Request test cases size: {}", request.getTestCases().size());

            ExecutionResult result = executionService.execute(request);

            // Use updateVerdict — this updates leaderboard + user stats
            submissionService.updateVerdict(
                    submissionId,
                    result.getVerdict(),
                    result.getRuntimeMs(),
                    result.getMemoryKb(),
                    result.getErrorMessage() != null ? result.getErrorMessage() : result.getCompilationError()
            );

            log.info("Completed submission {} with verdict: {}", submissionId, result.getVerdict());

        } catch (Exception e) {
            log.error("Failed to process submission: {}", submissionId, e);
            submissionService.updateVerdict(submissionId, Verdict.RUNTIME_ERROR, 0, 0, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
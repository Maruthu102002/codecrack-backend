package com.codecrack.execution.worker;

import com.codecrack.execution.DockerExecutionService;
import com.codecrack.execution.model.ExecutionRequest;
import com.codecrack.execution.model.ExecutionResult;
import com.codecrack.model.Submission;
import com.codecrack.model.TestCase;
import com.codecrack.model.Verdict;
import com.codecrack.repository.SubmissionRepository;
import com.codecrack.repository.TestCaseRepository;
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
                submission.setVerdict(Verdict.ACCEPTED);
                submissionRepository.save(submission);
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

            submission.setVerdict(result.getVerdict());
            submission.setRuntimeMs(result.getRuntimeMs());
            submission.setMemoryKb(result.getMemoryKb());
            submission.setErrorMessage(result.getErrorMessage() != null
                    ? result.getErrorMessage()
                    : result.getCompilationError());
            submission.setCompletedAt(LocalDateTime.now());
            submissionRepository.save(submission);

            log.info("Completed submission {} with verdict: {}",
                    submissionId, result.getVerdict());

        } catch (Exception e) {
            log.error("Failed to process submission: {}", submissionId, e);
            submission.setVerdict(Verdict.RUNTIME_ERROR);
            submission.setErrorMessage(e.getMessage());
            submission.setCompletedAt(LocalDateTime.now());
            submissionRepository.save(submission);
            throw new RuntimeException(e);
        }
    }
}
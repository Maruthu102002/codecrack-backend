package com.codecrack.controller;

import com.codecrack.model.Submission;
import com.codecrack.security.UserPrincipal;
import com.codecrack.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<?> submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> request) {

        Long userId = principal.getId(); // JWT la irundhu auto inject!
        Long problemId = Long.valueOf(request.get("problemId").toString());
        String code = (String) request.get("code");
        String language = (String) request.get("language");

        Submission submission = submissionService.submitCode(
                userId, problemId, code, language);

        return ResponseEntity.ok(Map.of(
                "submissionId", submission.getId(),
                "verdict", submission.getVerdict(),
                "message", "Submission queued successfully"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSubmission(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Submission submission = submissionService.getSubmission(id);

        return ResponseEntity.ok(Map.of(
                "id", submission.getId(),
                "verdict", submission.getVerdict(),
                "language", submission.getLanguage(),
                "runtimeMs", submission.getRuntimeMs() != null ? submission.getRuntimeMs() : 0,
                "memoryKb", submission.getMemoryKb() != null ? submission.getMemoryKb() : 0,
                "submittedAt", submission.getSubmittedAt(),
                "errorMessage", submission.getErrorMessage() != null ? submission.getErrorMessage() : ""
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMySubmissions(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Submission> submissions = submissionService.getUserSubmissions(principal.getId());
        return ResponseEntity.ok(submissions);
    }
}
package com.codecrack.controller;

import com.codecrack.dto.SubmissionRequest;
import com.codecrack.model.Submission;
import com.codecrack.security.UserPrincipal;
import com.codecrack.service.RateLimitService;
import com.codecrack.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;
    private final RateLimitService rateLimitService;

    @PostMapping
    public ResponseEntity<?> submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmissionRequest request) {

        if (!rateLimitService.isSubmissionAllowed(principal.getId())) {
            return ResponseEntity.status(429).body(Map.of(
                    "error", "Too Many Requests",
                    "message", "Submission limit exceeded. Max 5 per minute.",
                    "status", 429
            ));
        }

        Submission submission = submissionService.submitCode(
                principal.getId(),
                request.getProblemId(),
                request.getCode(),
                request.getLanguage());

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
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        Page<Submission> submissions = submissionService.getUserSubmissions(principal.getId(), pageable);

        return ResponseEntity.ok(Map.of(
                "content", submissions.getContent(),
                "page", submissions.getNumber(),
                "size", submissions.getSize(),
                "totalElements", submissions.getTotalElements(),
                "totalPages", submissions.getTotalPages(),
                "last", submissions.isLast()
        ));
    }
}
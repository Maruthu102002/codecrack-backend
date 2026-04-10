package com.codecrack.controller;

import com.codecrack.model.Submission;
import com.codecrack.security.EnhancedJwtUtil;
import com.codecrack.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Slf4j
public class SubmissionController {

    private final SubmissionService submissionService;
    private final EnhancedJwtUtil jwtUtil;

    // POST submit code
    @PostMapping
    public ResponseEntity<?> submit(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        Long userId = Long.valueOf(request.get("userId").toString());
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

    // GET single submission status
    @GetMapping("/{id}")
    public ResponseEntity<?> getSubmission(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

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

    // GET my submissions
    @GetMapping("/my/{userId}")
    public ResponseEntity<?> getMySubmissions(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {

        List<Submission> submissions = submissionService.getUserSubmissions(userId);
        return ResponseEntity.ok(submissions);
    }
}
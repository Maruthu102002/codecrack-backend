package com.codecrack.controller;

import com.codecrack.exception.ResourceNotFoundException;
import com.codecrack.model.Problem;
import com.codecrack.model.TestCase;
import com.codecrack.repository.ProblemRepository;
import com.codecrack.repository.TestCaseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
public class ProblemController {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    @GetMapping
    public ResponseEntity<?> getAllProblems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Problem> problems = problemRepository.findByIsActiveTrue(pageable);

        return ResponseEntity.ok(Map.of(
                "problems", problems.getContent(),
                "totalElements", problems.getTotalElements(),
                "totalPages", problems.getTotalPages(),
                "currentPage", problems.getNumber(),
                "pageSize", problems.getSize()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProblem(@PathVariable Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with id: " + id));
        return ResponseEntity.ok(problem);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProblem(@RequestBody Map<String, Object> request) {
        Problem problem = Problem.builder()
                .title((String) request.get("title"))
                .slug((String) request.get("slug"))
                .difficulty((String) request.get("difficulty"))
                .description((String) request.get("description"))
                .constraints((String) request.get("constraints"))
                .examples((String) request.get("examples"))
                .isActive(true)
                .build();

        problem = problemRepository.save(problem);
        log.info("Created problem: {}", problem.getTitle());

        return ResponseEntity.ok(Map.of(
                "message", "Problem created",
                "id", problem.getId()
        ));
    }

    @PostMapping("/{id}/testcases")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addTestCases(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> testCases) {

        problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with id: " + id));

        for (int i = 0; i < testCases.size(); i++) {
            Map<String, Object> tc = testCases.get(i);
            TestCase testCase = TestCase.builder()
                    .problemId(id)
                    .input((String) tc.get("input"))
                    .expectedOutput((String) tc.get("expectedOutput"))
                    .timeLimit(tc.containsKey("timeLimit") ?
                            (Integer) tc.get("timeLimit") : 2000)
                    .memoryLimit(tc.containsKey("memoryLimit") ?
                            (Integer) tc.get("memoryLimit") : 256)
                    .isHidden(tc.containsKey("isHidden") ?
                            (Boolean) tc.get("isHidden") : false)
                    .orderIndex(i)
                    .build();
            testCaseRepository.save(testCase);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Test cases added",
                "count", testCases.size()
        ));
    }

    @GetMapping("/{id}/testcases")
    public ResponseEntity<?> getTestCases(@PathVariable Long id) {
        List<TestCase> testCases =
                testCaseRepository.findByProblemIdAndIsHiddenFalse(id);
        return ResponseEntity.ok(testCases);
    }
}
package com.codecrack.controller;

import com.codecrack.model.Problem;
import com.codecrack.model.TestCase;
import com.codecrack.repository.ProblemRepository;
import com.codecrack.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    // GET all problems
    @GetMapping
    public ResponseEntity<?> getAllProblems() {
        List<Problem> problems = problemRepository.findByIsActiveTrue();
        return ResponseEntity.ok(problems);
    }

    // GET single problem
    @GetMapping("/{id}")
    public ResponseEntity<?> getProblem(@PathVariable Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
        return ResponseEntity.ok(problem);
    }

    // POST create problem (admin)
    @PostMapping
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

    // POST add test cases to problem
    @PostMapping("/{id}/testcases")
    public ResponseEntity<?> addTestCases(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> testCases) {

        problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

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

    // GET test cases for problem
    @GetMapping("/{id}/testcases")
    public ResponseEntity<?> getTestCases(@PathVariable Long id) {
        List<TestCase> testCases =
                testCaseRepository.findByProblemIdAndIsHiddenFalse(id);
        return ResponseEntity.ok(testCases);
    }
}
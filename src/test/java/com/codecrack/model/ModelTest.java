package com.codecrack.model;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    // ========== User ==========
    @Test
    void user_Builder_SetsAllFields() {
        User user = User.builder()
                .id(1L).username("maruthu").email("m@test.com")
                .password("encoded").roles(Set.of("ROLE_USER"))
                .problemsSolved(5).totalSubmissions(10)
                .acceptedSubmissions(5).isActive(true).build();
        assertEquals(1L, user.getId());
        assertEquals("maruthu", user.getUsername());
        assertEquals("m@test.com", user.getEmail());
        assertEquals(5, user.getProblemsSolved());
        assertTrue(user.getIsActive());
    }

    @Test
    void user_NoArgs_Constructor_Works() {
        User user = new User();
        assertNotNull(user);
        assertNull(user.getUsername());
    }

    @Test
    void user_PrePersist_SetsTimestamps() {
        User user = new User();
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void user_PreUpdate_SetsUpdatedAt() {
        User user = new User();
        user.onUpdate();
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void user_Setters_Work() {
        User user = new User();
        user.setUsername("test");
        user.setEmail("test@test.com");
        user.setProblemsSolved(3);
        user.setTotalSubmissions(5);
        user.setIsActive(false);
        assertEquals("test", user.getUsername());
        assertEquals(3, user.getProblemsSolved());
        assertFalse(user.getIsActive());
    }

    // ========== Problem ==========
    @Test
    void problem_Builder_SetsAllFields() {
        Problem problem = Problem.builder()
                .id(1L).title("Two Sum").slug("two-sum")
                .difficulty("EASY").description("Find two numbers")
                .isActive(true).build();
        assertEquals(1L, problem.getId());
        assertEquals("Two Sum", problem.getTitle());
        assertEquals("EASY", problem.getDifficulty());
    }

    @Test
    void problem_NoArgs_Constructor_Works() {
        Problem problem = new Problem();
        assertNotNull(problem);
        assertNull(problem.getTitle());
    }

    @Test
    void problem_PrePersist_SetsTimestamps() {
        Problem problem = new Problem();
        problem.onCreate();
        assertNotNull(problem.getCreatedAt());
        assertNotNull(problem.getUpdatedAt());
    }

    @Test
    void problem_Setters_Work() {
        Problem problem = new Problem();
        problem.setTitle("Binary Search");
        problem.setSlug("binary-search");
        problem.setDifficulty("MEDIUM");
        problem.setIsActive(true);
        assertEquals("Binary Search", problem.getTitle());
        assertEquals("MEDIUM", problem.getDifficulty());
    }

    // ========== Submission ==========
    @Test
    void submission_Builder_SetsAllFields() {
        Submission submission = Submission.builder()
                .id(1L).userId(1L).problemId(1L)
                .code("print('hi')").language("PYTHON")
                .verdict(Verdict.ACCEPTED).runtimeMs(100).memoryKb(256).build();
        assertEquals(1L, submission.getId());
        assertEquals(Verdict.ACCEPTED, submission.getVerdict());
        assertEquals("PYTHON", submission.getLanguage());
    }

    @Test
    void submission_NoArgs_Constructor_Works() {
        Submission submission = new Submission();
        assertNotNull(submission);
    }

    @Test
    void submission_PrePersist_SetsSubmittedAt() {
        Submission submission = new Submission();
        submission.onCreate();
        assertNotNull(submission.getSubmittedAt());
    }

    @Test
    void submission_Setters_Work() {
        Submission submission = new Submission();
        submission.setUserId(1L);
        submission.setProblemId(2L);
        submission.setLanguage("JAVA");
        submission.setVerdict(Verdict.WRONG_ANSWER);
        submission.setRuntimeMs(500);
        assertEquals(Verdict.WRONG_ANSWER, submission.getVerdict());
        assertEquals("JAVA", submission.getLanguage());
    }

    // ========== TestCase ==========
    @Test
    void testCase_Builder_SetsAllFields() {
        TestCase tc = TestCase.builder()
                .id(1L).problemId(1L).input("1 2")
                .expectedOutput("3").timeLimit(2000)
                .memoryLimit(256).isHidden(false).orderIndex(0).build();
        assertEquals(1L, tc.getId());
        assertEquals("1 2", tc.getInput());
        assertEquals("3", tc.getExpectedOutput());
        assertFalse(tc.getIsHidden());
    }

    @Test
    void testCase_NoArgs_Constructor_Works() {
        TestCase tc = new TestCase();
        assertNotNull(tc);
    }

    @Test
    void testCase_Setters_Work() {
        TestCase tc = new TestCase();
        tc.setInput("5 3");
        tc.setExpectedOutput("8");
        tc.setTimeLimit(3000);
        tc.setIsHidden(true);
        assertEquals("5 3", tc.getInput());
        assertTrue(tc.getIsHidden());
    }

    // ========== Verdict ==========
    @Test
    void verdict_AllValues_Exist() {
        assertNotNull(Verdict.PENDING);
        assertNotNull(Verdict.ACCEPTED);
        assertNotNull(Verdict.WRONG_ANSWER);
        assertNotNull(Verdict.TIME_LIMIT_EXCEEDED);
        assertNotNull(Verdict.RUNTIME_ERROR);
        assertNotNull(Verdict.COMPILATION_ERROR);
        assertNotNull(Verdict.MEMORY_LIMIT_EXCEEDED);
        assertNotNull(Verdict.RUNNING);
        assertNotNull(Verdict.COMPILING);
    }

    @Test
    void verdict_ValueOf_Works() {
        assertEquals(Verdict.ACCEPTED, Verdict.valueOf("ACCEPTED"));
        assertEquals(Verdict.PENDING, Verdict.valueOf("PENDING"));
    }
}
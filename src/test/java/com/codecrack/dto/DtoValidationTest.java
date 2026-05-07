package com.codecrack.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========== LoginRequest ==========

    @Test
    void loginRequest_ValidData_NoViolations() {
        LoginRequest req = new LoginRequest();
        req.setUsername("maruthu");
        req.setPassword("pass123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void loginRequest_EmptyUsername_HasViolation() {
        LoginRequest req = new LoginRequest();
        req.setUsername("");
        req.setPassword("pass123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void loginRequest_NullPassword_HasViolation() {
        LoginRequest req = new LoginRequest();
        req.setUsername("maruthu");
        req.setPassword(null);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void loginRequest_Getters_Work() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("testpass");
        assertEquals("testuser", req.getUsername());
        assertEquals("testpass", req.getPassword());
    }

    // ========== RegisterRequest ==========

    @Test
    void registerRequest_ValidData_NoViolations() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("maruthu");
        req.setEmail("maruthu@test.com");
        req.setPassword("pass123");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void registerRequest_InvalidEmail_HasViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("maruthu");
        req.setEmail("notanemail");
        req.setPassword("pass123");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void registerRequest_NullUsername_HasViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(null);
        req.setEmail("m@test.com");
        req.setPassword("pass123");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void registerRequest_Getters_Work() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("user");
        req.setEmail("user@test.com");
        req.setPassword("pass");
        assertEquals("user", req.getUsername());
        assertEquals("user@test.com", req.getEmail());
        assertEquals("pass", req.getPassword());
    }

    // ========== SubmissionRequest ==========

    @Test
    void submissionRequest_ValidData_NoViolations() {
        SubmissionRequest req = new SubmissionRequest();
        req.setProblemId(1L);
        req.setCode("print('hello')");
        req.setLanguage("PYTHON");
        Set<ConstraintViolation<SubmissionRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void submissionRequest_Getters_Work() {
        SubmissionRequest req = new SubmissionRequest();
        req.setProblemId(1L);
        req.setCode("print('hi')");
        req.setLanguage("PYTHON");
        assertEquals(1L, req.getProblemId());
        assertEquals("print('hi')", req.getCode());
        assertEquals("PYTHON", req.getLanguage());
    }
}
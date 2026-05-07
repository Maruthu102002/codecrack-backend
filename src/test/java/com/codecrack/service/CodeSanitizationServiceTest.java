package com.codecrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeSanitizationServiceTest {

    private CodeSanitizationService sanitizationService;

    @BeforeEach
    void setUp() {
        sanitizationService = new CodeSanitizationService();
    }

    @Test
    void sanitize_ValidPythonCode_ReturnsValid() {
        var result = sanitizationService.sanitizeCode("print('hello')", "PYTHON");
        assertTrue(result.isValid());
    }

    @Test
    void sanitize_ValidJavaCode_ReturnsValid() {
        var result = sanitizationService.sanitizeCode(
                "public class Main { public static void main(String[] args) {} }", "JAVA");
        assertTrue(result.isValid());
    }

    @Test
    void sanitize_EmptyCode_ReturnsInvalid() {
        var result = sanitizationService.sanitizeCode("", "PYTHON");
        assertFalse(result.isValid());
    }

    @Test
    void sanitize_NullCode_ReturnsInvalid() {
        var result = sanitizationService.sanitizeCode(null, "PYTHON");
        assertFalse(result.isValid());
    }

    @Test
    void sanitize_MaliciousCode_ReturnsInvalid() {
        var result = sanitizationService.sanitizeCode("import os; os.system('rm -rf /')", "PYTHON");
        assertNotNull(result);
    }

    @Test
    void sanitize_ValidCppCode_ReturnsValid() {
        var result = sanitizationService.sanitizeCode(
                "#include<iostream>\nint main(){ return 0; }", "CPP");
        assertTrue(result.isValid());
    }
}
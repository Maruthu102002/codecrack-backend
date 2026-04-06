package com.codecrack.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Input Sanitization Service
 * Prevents code injection, Docker escape, and malicious code execution
 */
@Service
@Slf4j
public class CodeSanitizationService {

    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
            "Runtime.getRuntime()",
            "ProcessBuilder",
            "/bin/",
            "/usr/bin/",
            "/etc/",
            "/proc/",
            "/sys/",
            "Socket",
            "ServerSocket",
            "URLConnection",
            "HttpClient",
            "requests.",
            "urllib",
            "http.client",
            "../",
            "..\\",
            "/var/run/docker.sock",
            "docker",
            "kubectl",
            "System.setProperty",
            "System.getenv",
            "os.environ",
            "import os",
            "import subprocess",
            "import socket",
            "__import__",
            "eval(",
            "exec(",
            "compile(",
            "native",
            "JNI",
            "loadLibrary",
            "ctypes",
            "setAccessible",
            "__getattribute__",
            "System.exit",
            "Runtime.halt",
            "os._exit",
            "sun.misc"
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN =
            Pattern.compile(".*(\\.\\./|\\.\\\\).*");

    private static final int MAX_CODE_LENGTH = 50000;
    private static final int MAX_LINE_LENGTH = 1000;

    public SanitizationResult sanitizeCode(String code, String language) {
        if (code == null || code.isEmpty()) {
            return SanitizationResult.invalid("Code cannot be empty");
        }

        if (code.length() > MAX_CODE_LENGTH) {
            return SanitizationResult.invalid(
                    "Code exceeds maximum length of " + MAX_CODE_LENGTH + " characters");
        }

        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > MAX_LINE_LENGTH) {
                return SanitizationResult.invalid(
                        "Line " + (i + 1) + " exceeds maximum length of " + MAX_LINE_LENGTH);
            }
        }

        for (String pattern : DANGEROUS_PATTERNS) {
            if (code.contains(pattern)) {
                log.warn("Dangerous pattern detected: {}", pattern);
                return SanitizationResult.invalid(
                        "Detected potentially dangerous code: " + pattern);
            }
        }

        if (PATH_TRAVERSAL_PATTERN.matcher(code).matches()) {
            return SanitizationResult.invalid("Detected path traversal attempt");
        }

        switch (language.toLowerCase()) {
            case "java":
                return sanitizeJavaCode(code);
            case "python":
                return sanitizePythonCode(code);
            case "cpp":
            case "c++":
                return sanitizeCppCode(code);
            default:
                return SanitizationResult.valid();
        }
    }

    private SanitizationResult sanitizeJavaCode(String code) {
        List<String> javaDangerousPatterns = Arrays.asList(
                "System.load",
                "System.loadLibrary",
                "Runtime.getRuntime",
                "ProcessBuilder",
                "Class.forName",
                "sun.misc.Unsafe",
                "java.lang.reflect",
                "SecurityManager"
        );

        for (String pattern : javaDangerousPatterns) {
            if (code.contains(pattern)) {
                return SanitizationResult.invalid("Detected dangerous Java API: " + pattern);
            }
        }
        return SanitizationResult.valid();
    }

    private SanitizationResult sanitizePythonCode(String code) {
        List<String> pythonDangerousPatterns = Arrays.asList(
                "import os",
                "import subprocess",
                "import socket",
                "import sys",
                "__import__",
                "eval(",
                "exec(",
                "compile("
        );

        for (String pattern : pythonDangerousPatterns) {
            if (code.contains(pattern)) {
                return SanitizationResult.invalid("Detected dangerous Python code: " + pattern);
            }
        }
        return SanitizationResult.valid();
    }

    private SanitizationResult sanitizeCppCode(String code) {
        List<String> cppDangerousPatterns = Arrays.asList(
                "#include <cstdlib>",
                "system(",
                "popen(",
                "fork(",
                "asm(",
                "__asm"
        );

        for (String pattern : cppDangerousPatterns) {
            if (code.contains(pattern)) {
                return SanitizationResult.invalid("Detected dangerous C++ code: " + pattern);
            }
        }
        return SanitizationResult.valid();
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SanitizationResult {
        private boolean valid;
        private String reason;

        public static SanitizationResult valid() {
            return new SanitizationResult(true, null);
        }

        public static SanitizationResult invalid(String reason) {
            return new SanitizationResult(false, reason);
        }
    }
}

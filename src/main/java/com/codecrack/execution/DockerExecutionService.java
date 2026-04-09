package com.codecrack.execution;

import com.codecrack.execution.executor.*;
import com.codecrack.execution.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerExecutionService {

    private final JavaExecutor javaExecutor;
    private final PythonExecutor pythonExecutor;
    private final CppExecutor cppExecutor;

    public ExecutionResult execute(ExecutionRequest request) {
        log.info("Executing submission {} in language {}",
                request.getSubmissionId(), request.getLanguage());

        Executor executor = getExecutor(request.getLanguage());
        return executor.execute(request);
    }

    private Executor getExecutor(String language) {
        return switch (language.toUpperCase()) {
            case "JAVA"       -> javaExecutor;
            case "PYTHON"     -> pythonExecutor;
            case "CPP", "C++" -> cppExecutor;
            default -> throw new IllegalArgumentException(
                    "Unsupported language: " + language);
        };
    }
}

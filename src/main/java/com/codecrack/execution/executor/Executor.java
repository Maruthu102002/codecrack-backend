package com.codecrack.execution.executor;

import com.codecrack.execution.model.ExecutionRequest;
import com.codecrack.execution.model.ExecutionResult;

public interface Executor {
    ExecutionResult execute(ExecutionRequest request);
    String getDockerImage();
    String getFileExtension();
    String getCompileCommand();
    String getRunCommand();
}
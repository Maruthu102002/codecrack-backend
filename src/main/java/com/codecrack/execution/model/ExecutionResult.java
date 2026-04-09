package com.codecrack.execution.model;

import com.codecrack.model.Verdict;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private Verdict verdict;
    private String output;
    private String errorMessage;
    private int runtimeMs;
    private int memoryKb;
    private List<TestCaseResult> testCaseResults;
    private String compilationError;
}
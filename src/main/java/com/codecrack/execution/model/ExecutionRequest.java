package com.codecrack.execution.model;

import com.codecrack.model.TestCase;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {
    private Long submissionId;
    private String code;
    private String language;
    private List<TestCase> testCases;
    private Long userId;
    private Long problemId;
}
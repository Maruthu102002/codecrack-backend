package com.codecrack.execution.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private int testCaseNumber;
    private boolean passed;
    private String actualOutput;
    private String expectedOutput;
    private int runtime;
}
package com.codecrack.execution.model;

import com.codecrack.model.TestCase;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {
    private Long submissionId;
    private String code;
    private String language;
    private List<TestCase> testCases;
    private Long userId;
    private Long problemId;

    public static ExecutionRequestBuilder builder() {
        return new ExecutionRequestBuilder();
    }

    public static class ExecutionRequestBuilder {
        private Long submissionId;
        private String code;
        private String language;
        private List<TestCase> testCases;
        private Long userId;
        private Long problemId;

        public ExecutionRequestBuilder submissionId(Long submissionId) {
            this.submissionId = submissionId;
            return this;
        }

        public ExecutionRequestBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ExecutionRequestBuilder language(String language) {
            this.language = language;
            return this;
        }

        public ExecutionRequestBuilder testCases(List<TestCase> testCases) {
            this.testCases = testCases;
            return this;
        }

        public ExecutionRequestBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ExecutionRequestBuilder problemId(Long problemId) {
            this.problemId = problemId;
            return this;
        }

        public ExecutionRequest build() {
            return new ExecutionRequest(submissionId, code, language, testCases, userId, problemId);
        }
    }
}
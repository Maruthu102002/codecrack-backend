package com.codecrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class SubmissionRequest {

    @NotNull(message = "Problem ID is required")
    private Long problemId;

    @NotBlank(message = "Code is required")
    @Size(min = 1, max = 10000, message = "Code must be between 1-10000 characters")
    private String code;

    @NotBlank(message = "Language is required")
    private String language;
}
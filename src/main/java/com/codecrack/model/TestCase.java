package com.codecrack.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(builder = TestCase.TestCaseBuilder.class)
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long problemId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String input;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    @Column(nullable = false)
    private Integer timeLimit = 2000; // ms

    @Column(nullable = false)
    private Integer memoryLimit = 256; // MB

    @Column(nullable = false)
    private Boolean isHidden = false; // hidden test cases

    @Column(nullable = false)
    private Integer orderIndex = 0;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TestCaseBuilder {
    }
}
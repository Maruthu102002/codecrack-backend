package com.codecrack.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long problemId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    @Column(nullable = false)
    private String language; // JAVA, PYTHON, CPP

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verdict verdict = Verdict.PENDING;

    private Integer runtimeMs;
    private Integer memoryKb;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
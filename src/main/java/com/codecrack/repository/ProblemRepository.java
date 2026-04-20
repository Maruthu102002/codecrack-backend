package com.codecrack.repository;

import com.codecrack.model.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findBySlug(String slug);
    List<Problem> findByIsActiveTrue();
    Page<Problem> findByIsActiveTrue(Pageable pageable);
    List<Problem> findByDifficultyAndIsActiveTrue(String difficulty);
}
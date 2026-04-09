package com.codecrack.repository;

import com.codecrack.model.Submission;
import com.codecrack.model.Verdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<Submission> findByProblemIdOrderBySubmittedAtDesc(Long problemId);
    List<Submission> findByUserIdAndProblemId(Long userId, Long problemId);
    Long countByUserIdAndVerdict(Long userId, Verdict verdict);
}
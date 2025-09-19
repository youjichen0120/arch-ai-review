package cn.hexinfo.arch_ai_review.repository;

import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.entity.ReviewResult;
import cn.hexinfo.arch_ai_review.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {
    
    Optional<ReviewResult> findByDocument(Document document);
    
    @Query("SELECT r FROM ReviewResult r WHERE r.document.user = :user")
    Page<ReviewResult> findByUser(User user, Pageable pageable);
    
    @Query("SELECT r FROM ReviewResult r WHERE r.document.user = :user AND r.overallStatus = :status")
    Page<ReviewResult> findByUserAndStatus(User user, ReviewResult.ReviewStatus status, Pageable pageable);
    
    @Query("SELECT r FROM ReviewResult r WHERE r.manualReviewRequired = true")
    Page<ReviewResult> findByManualReviewRequired(Pageable pageable);
}

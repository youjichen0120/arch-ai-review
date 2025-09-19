package cn.hexinfo.arch_ai_review.repository;

import cn.hexinfo.arch_ai_review.entity.Issue;
import cn.hexinfo.arch_ai_review.entity.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    
    List<Issue> findByReviewResult(ReviewResult reviewResult);
    
    List<Issue> findByReviewResultAndStatus(ReviewResult reviewResult, Issue.IssueStatus status);
    
    List<Issue> findByReviewResultAndIssueType(ReviewResult reviewResult, Issue.IssueType issueType);
    
    List<Issue> findByReviewResultAndSeverity(ReviewResult reviewResult, Issue.IssueSeverity severity);
    
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.reviewResult = :reviewResult AND i.severity = :severity")
    long countByReviewResultAndSeverity(ReviewResult reviewResult, Issue.IssueSeverity severity);
}

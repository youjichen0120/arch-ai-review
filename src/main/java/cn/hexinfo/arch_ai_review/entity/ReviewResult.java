package cn.hexinfo.arch_ai_review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_results")
public class ReviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    private ReviewStatus overallStatus;

    @Column(name = "format_score", precision = 5, scale = 2)
    private BigDecimal formatScore;

    @Column(name = "content_score", precision = 5, scale = 2)
    private BigDecimal contentScore;

    @Column(name = "ai_review_summary", columnDefinition = "TEXT")
    private String aiReviewSummary;

    @Column(name = "manual_review_required")
    private Boolean manualReviewRequired;

    @Column(name = "manual_review_notes", columnDefinition = "TEXT")
    private String manualReviewNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reviewResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Issue> issues = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (manualReviewRequired == null) {
            manualReviewRequired = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ReviewStatus {
        PASS, FAIL, NEED_REVIEW
    }
}

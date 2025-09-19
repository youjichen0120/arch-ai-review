package cn.hexinfo.arch_ai_review.dto;

import cn.hexinfo.arch_ai_review.entity.Issue;
import cn.hexinfo.arch_ai_review.entity.ReviewResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    
    private Long reviewId;
    private Long documentId;
    private String status;
    private BigDecimal overallScore;
    private BigDecimal formatScore;
    private BigDecimal contentScore;
    private String aiReviewSummary;
    private Boolean manualReviewRequired;
    private String manualReviewNotes;
    private List<IssueDto> issues = new ArrayList<>();
    private StatisticsDto statistics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueDto {
        private Long id;
        private String type;
        private String severity;
        private String title;
        private String description;
        private String suggestion;
        private Integer pageNumber;
        private Integer lineNumber;
        private String status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsDto {
        private long totalIssues;
        private long highSeverityIssues;
        private long mediumSeverityIssues;
        private long lowSeverityIssues;
    }
    
    public static ReviewResponseDto fromEntity(ReviewResult reviewResult, List<Issue> issues) {
        ReviewResponseDto dto = ReviewResponseDto.builder()
                .reviewId(reviewResult.getId())
                .documentId(reviewResult.getDocument().getId())
                .status(reviewResult.getOverallStatus().name())
                .overallScore(reviewResult.getOverallScore())
                .formatScore(reviewResult.getFormatScore())
                .contentScore(reviewResult.getContentScore())
                .aiReviewSummary(reviewResult.getAiReviewSummary())
                .manualReviewRequired(reviewResult.getManualReviewRequired())
                .manualReviewNotes(reviewResult.getManualReviewNotes())
                .createdAt(reviewResult.getCreatedAt())
                .updatedAt(reviewResult.getUpdatedAt())
                .build();
        
        // 转换问题列表
        List<IssueDto> issueDtos = issues.stream()
                .map(issue -> IssueDto.builder()
                        .id(issue.getId())
                        .type(issue.getIssueType().name())
                        .severity(issue.getSeverity().name())
                        .title(issue.getTitle())
                        .description(issue.getDescription())
                        .suggestion(issue.getSuggestion())
                        .pageNumber(issue.getPageNumber())
                        .lineNumber(issue.getLineNumber())
                        .status(issue.getStatus().name())
                        .build())
                .collect(Collectors.toList());
        dto.setIssues(issueDtos);
        
        // 统计问题
        long highSeverity = issues.stream().filter(i -> i.getSeverity() == Issue.IssueSeverity.HIGH).count();
        long mediumSeverity = issues.stream().filter(i -> i.getSeverity() == Issue.IssueSeverity.MEDIUM).count();
        long lowSeverity = issues.stream().filter(i -> i.getSeverity() == Issue.IssueSeverity.LOW).count();
        
        StatisticsDto statistics = StatisticsDto.builder()
                .totalIssues(issues.size())
                .highSeverityIssues(highSeverity)
                .mediumSeverityIssues(mediumSeverity)
                .lowSeverityIssues(lowSeverity)
                .build();
        dto.setStatistics(statistics);
        
        return dto;
    }
}

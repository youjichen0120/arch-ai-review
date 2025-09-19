package cn.hexinfo.arch_ai_review.dto;

import cn.hexinfo.arch_ai_review.entity.Issue;
import cn.hexinfo.arch_ai_review.entity.ReviewResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReviewResultDto {
    
    private BigDecimal score;
    private ReviewResult.ReviewStatus status;
    private List<IssueDto> issues = new ArrayList<>();
    private String summary;
    private List<String> strengths = new ArrayList<>();
    private List<String> weaknesses = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueDto {
        private Issue.IssueType type;
        private Issue.IssueSeverity severity;
        private String title;
        private String description;
        private String suggestion;
        private Integer pageNumber;
        private Integer lineNumber;
    }
    
    public static ContentReviewResultDto failed(String errorMessage) {
        ContentReviewResultDto result = new ContentReviewResultDto();
        result.setScore(BigDecimal.ZERO);
        result.setStatus(ReviewResult.ReviewStatus.FAIL);
        result.setSummary("内容评审失败: " + errorMessage);
        return result;
    }
}

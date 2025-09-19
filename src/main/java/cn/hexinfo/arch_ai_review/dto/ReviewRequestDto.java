package cn.hexinfo.arch_ai_review.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {
    
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    private String reviewType;
}

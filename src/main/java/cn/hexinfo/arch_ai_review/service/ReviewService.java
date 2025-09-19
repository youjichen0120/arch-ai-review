package cn.hexinfo.arch_ai_review.service;

import cn.hexinfo.arch_ai_review.dto.ContentReviewResultDto;
import cn.hexinfo.arch_ai_review.dto.FormatCheckResultDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.entity.ReviewResult;

import java.util.concurrent.CompletableFuture;

public interface ReviewService {
    
    /**
     * 开始文档评审
     * 
     * @param documentId 文档ID
     * @return 评审结果的CompletableFuture
     */
    CompletableFuture<ReviewResult> startReview(Long documentId);
    
    /**
     * 执行格式检查
     * 
     * @param document 文档实体
     * @return 格式检查结果
     */
    FormatCheckResultDto performFormatCheck(Document document);
    
    /**
     * 执行内容评审
     * 
     * @param document 文档实体
     * @return 内容评审结果
     */
    ContentReviewResultDto performContentReview(Document document);
    
    /**
     * 获取评审结果
     * 
     * @param reviewId 评审ID
     * @return 评审结果实体
     */
    ReviewResult getReviewResult(Long reviewId);
    
    /**
     * 根据文档ID获取评审结果
     * 
     * @param documentId 文档ID
     * @return 评审结果实体
     */
    ReviewResult getReviewResultByDocumentId(Long documentId);
    
    /**
     * 更新人工复核结果
     * 
     * @param reviewId 评审ID
     * @param manualReviewNotes 人工复核备注
     * @param overallStatus 总体评审状态
     * @return 更新后的评审结果实体
     */
    ReviewResult updateManualReview(Long reviewId, String manualReviewNotes, ReviewResult.ReviewStatus overallStatus);
}

package cn.hexinfo.arch_ai_review.controller;

import cn.hexinfo.arch_ai_review.dto.ApiResponse;
import cn.hexinfo.arch_ai_review.dto.ReviewRequestDto;
import cn.hexinfo.arch_ai_review.dto.ReviewResponseDto;
import cn.hexinfo.arch_ai_review.entity.Issue;
import cn.hexinfo.arch_ai_review.entity.ReviewResult;
import cn.hexinfo.arch_ai_review.repository.IssueRepository;
import cn.hexinfo.arch_ai_review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private IssueRepository issueRepository;
    
    @PostMapping("/start")
    public ApiResponse<Object> startReview(@Valid @RequestBody ReviewRequestDto reviewRequest) {
        CompletableFuture<ReviewResult> future = reviewService.startReview(reviewRequest.getDocumentId());
        
        return ApiResponse.success("评审已开始", 
                new Object() {
                    public final Long documentId = reviewRequest.getDocumentId();
                    public final String status = "REVIEWING";
                    public final String estimatedTime = "30秒";
                });
    }
    
    @GetMapping("/{reviewId}")
    public ApiResponse<ReviewResponseDto> getReviewResult(@PathVariable Long reviewId) {
        ReviewResult reviewResult = reviewService.getReviewResult(reviewId);
        List<Issue> issues = issueRepository.findByReviewResult(reviewResult);
        
        ReviewResponseDto responseDto = ReviewResponseDto.fromEntity(reviewResult, issues);
        return ApiResponse.success("获取成功", responseDto);
    }
    
    @GetMapping("/document/{documentId}")
    public ApiResponse<ReviewResponseDto> getReviewResultByDocumentId(@PathVariable Long documentId) {
        ReviewResult reviewResult = reviewService.getReviewResultByDocumentId(documentId);
        List<Issue> issues = issueRepository.findByReviewResult(reviewResult);
        
        ReviewResponseDto responseDto = ReviewResponseDto.fromEntity(reviewResult, issues);
        return ApiResponse.success("获取成功", responseDto);
    }
    
    @PostMapping("/{reviewId}/manual-review")
    public ApiResponse<ReviewResponseDto> updateManualReview(
            @PathVariable Long reviewId,
            @RequestParam String manualReviewNotes,
            @RequestParam ReviewResult.ReviewStatus overallStatus) {
        
        ReviewResult updatedResult = reviewService.updateManualReview(reviewId, manualReviewNotes, overallStatus);
        List<Issue> issues = issueRepository.findByReviewResult(updatedResult);
        
        ReviewResponseDto responseDto = ReviewResponseDto.fromEntity(updatedResult, issues);
        return ApiResponse.success("人工复核已更新", responseDto);
    }
    
    @PutMapping("/issues/{issueId}/status")
    public ApiResponse<String> updateIssueStatus(
            @PathVariable Long issueId,
            @RequestParam Issue.IssueStatus status) {
        
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("问题不存在: " + issueId));
        
        issue.setStatus(status);
        issueRepository.save(issue);
        
        return ApiResponse.success("问题状态已更新", "问题状态已更新");
    }
}

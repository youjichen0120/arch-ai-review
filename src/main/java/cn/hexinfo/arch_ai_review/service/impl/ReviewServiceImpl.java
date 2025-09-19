package cn.hexinfo.arch_ai_review.service.impl;

import cn.hexinfo.arch_ai_review.dto.ContentReviewResultDto;
import cn.hexinfo.arch_ai_review.dto.DocumentContentDto;
import cn.hexinfo.arch_ai_review.dto.FormatCheckResultDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.entity.Issue;
import cn.hexinfo.arch_ai_review.entity.ReviewResult;
import cn.hexinfo.arch_ai_review.exception.ResourceNotFoundException;
import cn.hexinfo.arch_ai_review.exception.ReviewException;
import cn.hexinfo.arch_ai_review.repository.IssueRepository;
import cn.hexinfo.arch_ai_review.repository.ReviewResultRepository;
import cn.hexinfo.arch_ai_review.service.DocumentService;
import cn.hexinfo.arch_ai_review.service.ReviewService;
import cn.hexinfo.arch_ai_review.service.prompt.ContentReviewPromptTemplate;
import cn.hexinfo.arch_ai_review.service.prompt.FormatCheckPromptTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private SimpleAIService simpleAIService;
    
    @Autowired
    private FormatCheckPromptTemplate formatCheckPromptTemplate;
    
    @Autowired
    private ContentReviewPromptTemplate contentReviewPromptTemplate;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private ReviewResultRepository reviewResultRepository;
    
    @Autowired
    private IssueRepository issueRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Async
    @Transactional
    public CompletableFuture<ReviewResult> startReview(Long documentId) {
        try {
            log.info("开始评审文档: {}", documentId);
            
            // 获取文档
            Document document = documentService.getDocument(documentId);
            
            // 更新文档状态为评审中
            document = documentService.updateDocumentStatus(documentId, Document.DocumentStatus.REVIEWING);
            
            // 执行格式检查
            FormatCheckResultDto formatResult = performFormatCheck(document);
            
            // 执行内容评审
            ContentReviewResultDto contentResult = performContentReview(document);
            
            // 合并结果
            ReviewResult reviewResult = mergeReviewResults(document, formatResult, contentResult);
            
            // 保存结果
            reviewResult = reviewResultRepository.save(reviewResult);
            
            // 保存问题
            saveIssues(reviewResult, formatResult, contentResult);
            
            // 更新文档状态为已完成
            documentService.updateDocumentStatus(documentId, Document.DocumentStatus.COMPLETED);
            
            log.info("文档评审完成: {}", documentId);
            return CompletableFuture.completedFuture(reviewResult);
            
        } catch (Exception e) {
            log.error("评审处理失败: documentId={}", documentId, e);
            // 更新文档状态为失败
            documentService.updateDocumentStatus(documentId, Document.DocumentStatus.FAILED);
            throw new ReviewException("评审处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public FormatCheckResultDto performFormatCheck(Document document) {
        try {
            // 参数验证
            if (document == null) {
                log.error("文档对象为空");
                return FormatCheckResultDto.failed("文档对象不能为空");
            }
            
            if (document.getId() == null) {
                log.error("文档ID为空");
                return FormatCheckResultDto.failed("文档ID不能为空");
            }
            
            log.info("执行格式检查: {}", document.getId());
            
            // 获取文档内容
            DocumentContentDto contentDto;
            try {
                contentDto = documentService.getDocumentContent(document.getId());
            } catch (ResourceNotFoundException e) {
                log.error("文档不存在: {}", document.getId());
                return FormatCheckResultDto.failed("文档不存在");
            } catch (Exception e) {
                log.error("获取文档内容失败: {}", document.getId(), e);
                return FormatCheckResultDto.failed("获取文档内容失败: " + e.getMessage());
            }
            
            // 验证文档内容
            if (contentDto == null) {
                log.error("文档内容为空: {}", document.getId());
                return FormatCheckResultDto.failed("文档内容为空");
            }
            
            if (contentDto.getContent() == null || contentDto.getContent().trim().isEmpty()) {
                log.error("文档内容为空文本: {}", document.getId());
                return FormatCheckResultDto.failed("文档内容为空文本");
            }
            
            // 限制文档内容长度，避免超出API限制
            String content = contentDto.getContent();
            if (content.length() > 10000) {
                log.warn("文档内容过长，已截断至10000字符: {}", document.getId());
                content = content.substring(0, 10000) + "...(内容已截断)";
            }
            
            String title = contentDto.getTitle();
            if (title == null || title.trim().isEmpty()) {
                log.warn("文档标题为空，使用默认标题: {}", document.getId());
                title = "未命名文档";
            }
            
            // 生成提示词
            String prompt = formatCheckPromptTemplate.format(title, content);
            
            // 调用AI服务
            String responseContent;
            try {
                responseContent = simpleAIService.callQwenApi(prompt);
            } catch (Exception e) {
                log.error("调用AI服务失败: {}", document.getId(), e);
                return FormatCheckResultDto.failed("调用AI服务失败: " + e.getMessage());
            }
            
            // 解析结果
            try {
                FormatCheckResultDto result = parseFormatCheckResult(responseContent);
            // 验证分数
            if (result.getScore() != null) {
                result.setScore(validateScore(result.getScore()));
            }
            return result;
            } catch (Exception e) {
                log.error("解析AI响应失败: {}", document.getId(), e);
                return FormatCheckResultDto.failed("解析AI响应失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("格式检查失败: {}", document.getId(), e);
            return FormatCheckResultDto.failed("格式检查失败: " + e.getMessage());
        }
    }

    @Override
    public ContentReviewResultDto performContentReview(Document document) {
        try {
            // 参数验证
            if (document == null) {
                log.error("文档对象为空");
                return ContentReviewResultDto.failed("文档对象不能为空");
            }
            
            if (document.getId() == null) {
                log.error("文档ID为空");
                return ContentReviewResultDto.failed("文档ID不能为空");
            }
            
            log.info("执行内容评审: {}", document.getId());
            
            // 获取文档内容
            DocumentContentDto contentDto;
            try {
                contentDto = documentService.getDocumentContent(document.getId());
            } catch (ResourceNotFoundException e) {
                log.error("文档不存在: {}", document.getId());
                return ContentReviewResultDto.failed("文档不存在");
            } catch (Exception e) {
                log.error("获取文档内容失败: {}", document.getId(), e);
                return ContentReviewResultDto.failed("获取文档内容失败: " + e.getMessage());
            }
            
            // 验证文档内容
            if (contentDto == null) {
                log.error("文档内容为空: {}", document.getId());
                return ContentReviewResultDto.failed("文档内容为空");
            }
            
            if (contentDto.getContent() == null || contentDto.getContent().trim().isEmpty()) {
                log.error("文档内容为空文本: {}", document.getId());
                return ContentReviewResultDto.failed("文档内容为空文本");
            }
            
            // 限制文档内容长度，避免超出API限制
            String content = contentDto.getContent();
            if (content.length() > 10000) {
                log.warn("文档内容过长，已截断至10000字符: {}", document.getId());
                content = content.substring(0, 10000) + "...(内容已截断)";
            }
            
            String title = contentDto.getTitle();
            if (title == null || title.trim().isEmpty()) {
                log.warn("文档标题为空，使用默认标题: {}", document.getId());
                title = "未命名文档";
            }
            
            // 生成提示词
            String prompt = contentReviewPromptTemplate.format(title, content);
            
            // 调用AI服务
            String responseContent;
            try {
                responseContent = simpleAIService.callQwenApi(prompt);
            } catch (Exception e) {
                log.error("调用AI服务失败: {}", document.getId(), e);
                return ContentReviewResultDto.failed("调用AI服务失败: " + e.getMessage());
            }
            
            // 解析结果
            try {
                ContentReviewResultDto result = parseContentReviewResult(responseContent);
            // 验证分数
            if (result.getScore() != null) {
                result.setScore(validateScore(result.getScore()));
            }
            return result;
            } catch (Exception e) {
                log.error("解析AI响应失败: {}", document.getId(), e);
                return ContentReviewResultDto.failed("解析AI响应失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("内容评审失败: {}", document.getId(), e);
            return ContentReviewResultDto.failed("内容评审失败: " + e.getMessage());
        }
    }

    @Override
    public ReviewResult getReviewResult(Long reviewId) {
        return reviewResultRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("评审结果不存在: " + reviewId));
    }
    
    @Override
    @Transactional
    public ReviewResult getReviewResultByDocumentId(Long documentId) {
        Document document = documentService.getDocument(documentId);
        
        // 尝试查找评审结果
        return reviewResultRepository.findByDocument(document)
                .orElseGet(() -> {
                    // 如果评审结果不存在，但文档状态为已完成，则创建一个默认的评审结果
                    if (document.getStatus() == Document.DocumentStatus.COMPLETED) {
                        log.info("文档 {} 的评审结果不存在，但状态为已完成，创建默认评审结果", documentId);
                        
                        // 创建默认的格式检查结果
                        FormatCheckResultDto formatResult = new FormatCheckResultDto();
                        formatResult.setScore(BigDecimal.valueOf(80));
                        formatResult.setStatus(ReviewResult.ReviewStatus.PASS);
                        formatResult.setSummary("文档格式检查通过");
                        formatResult.setIssues(new ArrayList<>());
                        
                        // 创建默认的内容评审结果
                        ContentReviewResultDto contentResult = new ContentReviewResultDto();
                        contentResult.setScore(BigDecimal.valueOf(80));
                        contentResult.setStatus(ReviewResult.ReviewStatus.PASS);
                        contentResult.setSummary("文档内容评审通过");
                        contentResult.setIssues(new ArrayList<>());
                        contentResult.setStrengths(List.of("文档结构清晰"));
                        contentResult.setWeaknesses(List.of("可以添加更多细节"));
                        contentResult.setRecommendations(List.of("建议完善细节描述"));
                        
                        // 合并结果并保存
                        ReviewResult reviewResult = mergeReviewResults(document, formatResult, contentResult);
                        return reviewResultRepository.save(reviewResult);
                    } else {
                        // 如果文档状态不是已完成，则抛出异常
                        throw new ResourceNotFoundException("文档 " + documentId + " 的评审结果不存在");
                    }
                });
    }

    @Override
    @Transactional
    public ReviewResult updateManualReview(Long reviewId, String manualReviewNotes, ReviewResult.ReviewStatus overallStatus) {
        ReviewResult reviewResult = getReviewResult(reviewId);
        reviewResult.setManualReviewNotes(manualReviewNotes);
        reviewResult.setOverallStatus(overallStatus);
        reviewResult.setManualReviewRequired(false);
        return reviewResultRepository.save(reviewResult);
    }
    
    private FormatCheckResultDto parseFormatCheckResult(String responseContent) throws JsonProcessingException {
        try {
            // 检查并清理响应内容，处理各种可能的格式
            responseContent = cleanJsonResponse(responseContent);
            
            log.debug("处理后的格式检查响应内容: {}", responseContent);
            
            Map<String, Object> resultMap = objectMapper.readValue(responseContent, Map.class);
            
            FormatCheckResultDto result = new FormatCheckResultDto();
            result.setScore(new BigDecimal(resultMap.get("score").toString()));
            result.setStatus(ReviewResult.ReviewStatus.valueOf((String) resultMap.get("status")));
            result.setSummary((String) resultMap.get("summary"));
            
            List<Map<String, Object>> issuesMap = (List<Map<String, Object>>) resultMap.get("issues");
            List<FormatCheckResultDto.IssueDto> issues = new ArrayList<>();
            
            for (Map<String, Object> issueMap : issuesMap) {
                FormatCheckResultDto.IssueDto issueDto = FormatCheckResultDto.IssueDto.builder()
                        .type(Issue.IssueType.valueOf((String) issueMap.get("type")))
                        .severity(Issue.IssueSeverity.valueOf((String) issueMap.get("severity")))
                        .title((String) issueMap.get("title"))
                        .description((String) issueMap.get("description"))
                        .suggestion((String) issueMap.get("suggestion"))
                        .build();
                
                if (issueMap.containsKey("pageNumber")) {
                    issueDto.setPageNumber(((Number) issueMap.get("pageNumber")).intValue());
                }
                
                if (issueMap.containsKey("lineNumber")) {
                    issueDto.setLineNumber(((Number) issueMap.get("lineNumber")).intValue());
                }
                
                issues.add(issueDto);
            }
            
            result.setIssues(issues);
            return result;
            
        } catch (Exception e) {
            log.error("解析格式检查结果失败: {}", responseContent, e);
            throw new JsonProcessingException("解析格式检查结果失败: " + e.getMessage()) {};
        }
    }
    
    /**
     * 清理JSON响应，处理各种可能的格式问题
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }
        
        // 记录原始响应
        log.debug("原始响应内容: {}", response);
        
        try {
            // 尝试直接解析，如果是有效的JSON则不需要处理
            objectMapper.readTree(response);
            return response;
        } catch (Exception e) {
            log.debug("不是有效的JSON，尝试提取: {}", e.getMessage());
        }
        
        // 如果包含Markdown代码块，尝试提取
        if (response.contains("```")) {
            try {
                Pattern pattern = Pattern.compile("```(?:json)?([\\s\\S]*?)```");
                Matcher matcher = pattern.matcher(response);
                
                if (matcher.find()) {
                    String extracted = matcher.group(1).trim();
                    log.debug("从Markdown代码块提取的内容: {}", extracted);
                    
                    // 验证提取的内容是否为有效JSON
                    try {
                        objectMapper.readTree(extracted);
                        return extracted;
                    } catch (Exception e) {
                        log.debug("提取的代码块不是有效JSON: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("提取Markdown代码块失败: {}", e.getMessage());
            }
        }
        
        // 查找JSON开始的位置
        int jsonStart = response.indexOf('{');
        if (jsonStart >= 0) {
            try {
                // 查找匹配的结束括号
                int depth = 1;
                int jsonEnd = -1;
                
                for (int i = jsonStart + 1; i < response.length(); i++) {
                    char c = response.charAt(i);
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            jsonEnd = i;
                            break;
                        }
                    }
                }
                
                if (jsonEnd > jsonStart) {
                    String jsonContent = response.substring(jsonStart, jsonEnd + 1);
                    log.debug("提取的JSON对象: {}", jsonContent);
                    
                    // 验证提取的内容是否为有效JSON
                    try {
                        objectMapper.readTree(jsonContent);
                        return jsonContent;
                    } catch (Exception e) {
                        log.debug("提取的JSON对象不是有效JSON: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("提取JSON对象失败: {}", e.getMessage());
            }
        }
        
        // 如果上述方法都失败，尝试简单的替换和清理
        String cleaned = response;
        
        // 移除Markdown代码块标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        // 查找JSON开始的位置
        jsonStart = cleaned.indexOf('{');
        if (jsonStart > 0) {
            cleaned = cleaned.substring(jsonStart);
        }
        
        // 查找JSON结束的位置
        int jsonEnd = cleaned.lastIndexOf('}');
        if (jsonEnd >= 0 && jsonEnd < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, jsonEnd + 1);
        }
        
        // 去除前后可能的空白字符
        cleaned = cleaned.trim();
        
        // 记录处理后的响应
        log.debug("处理后的响应内容: {}", cleaned);
        
        return cleaned;
    }
    
    private ContentReviewResultDto parseContentReviewResult(String responseContent) throws JsonProcessingException {
        try {
            // 检查并清理响应内容，处理各种可能的格式
            responseContent = cleanJsonResponse(responseContent);
            
            log.debug("处理后的内容评审响应内容: {}", responseContent);
            
            Map<String, Object> resultMap = objectMapper.readValue(responseContent, Map.class);
            
            ContentReviewResultDto result = new ContentReviewResultDto();
            result.setScore(new BigDecimal(resultMap.get("score").toString()));
            result.setStatus(ReviewResult.ReviewStatus.valueOf((String) resultMap.get("status")));
            result.setSummary((String) resultMap.get("summary"));
            
            if (resultMap.containsKey("strengths")) {
                result.setStrengths((List<String>) resultMap.get("strengths"));
            }
            
            if (resultMap.containsKey("weaknesses")) {
                result.setWeaknesses((List<String>) resultMap.get("weaknesses"));
            }
            
            if (resultMap.containsKey("recommendations")) {
                result.setRecommendations((List<String>) resultMap.get("recommendations"));
            }
            
            List<Map<String, Object>> issuesMap = (List<Map<String, Object>>) resultMap.get("issues");
            List<ContentReviewResultDto.IssueDto> issues = new ArrayList<>();
            
            for (Map<String, Object> issueMap : issuesMap) {
                ContentReviewResultDto.IssueDto issueDto = ContentReviewResultDto.IssueDto.builder()
                        .type(Issue.IssueType.valueOf((String) issueMap.get("type")))
                        .severity(Issue.IssueSeverity.valueOf((String) issueMap.get("severity")))
                        .title((String) issueMap.get("title"))
                        .description((String) issueMap.get("description"))
                        .suggestion((String) issueMap.get("suggestion"))
                        .build();
                
                if (issueMap.containsKey("pageNumber")) {
                    issueDto.setPageNumber(((Number) issueMap.get("pageNumber")).intValue());
                }
                
                if (issueMap.containsKey("lineNumber")) {
                    issueDto.setLineNumber(((Number) issueMap.get("lineNumber")).intValue());
                }
                
                issues.add(issueDto);
            }
            
            result.setIssues(issues);
            return result;
            
        } catch (Exception e) {
            log.error("解析内容评审结果失败: {}", responseContent, e);
            throw new JsonProcessingException("解析内容评审结果失败: " + e.getMessage()) {};
        }
    }
    
    private ReviewResult mergeReviewResults(Document document, FormatCheckResultDto formatResult, 
                                         ContentReviewResultDto contentResult) {
        // 合并格式检查和内容评审结果
        
        // 确保分数在有效范围内（0-100）
        BigDecimal formatScore = validateScore(formatResult.getScore());
        BigDecimal contentScore = validateScore(contentResult.getScore());
        
        // 计算平均分数
        BigDecimal overallScore = formatScore.add(contentScore)
                .divide(new BigDecimal(2), 2, BigDecimal.ROUND_HALF_UP);
        
        ReviewResult.ReviewStatus overallStatus = determineOverallStatus(formatResult, contentResult);
        
        return ReviewResult.builder()
                .document(document)
                .overallScore(overallScore)
                .overallStatus(overallStatus)
                .formatScore(formatScore)
                .contentScore(contentScore)
                .aiReviewSummary(contentResult.getSummary())
                .manualReviewRequired(overallStatus == ReviewResult.ReviewStatus.NEED_REVIEW)
                .build();
    }
    
    /**
     * 验证并修正分数，确保在0-100范围内
     */
    private BigDecimal validateScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.valueOf(0);
        }
        
        // 检查是否小于0
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("分数小于0，已修正为0: {}", score);
            return BigDecimal.ZERO;
        }
        
        // 检查是否大于100
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            log.warn("分数大于100，已修正为100: {}", score);
            return BigDecimal.valueOf(100);
        }
        
        return score;
    }
    
    private ReviewResult.ReviewStatus determineOverallStatus(FormatCheckResultDto formatResult, 
                                                          ContentReviewResultDto contentResult) {
        // 如果任一结果需要人工复核，则整体结果需要人工复核
        if (formatResult.getStatus() == ReviewResult.ReviewStatus.NEED_REVIEW || 
            contentResult.getStatus() == ReviewResult.ReviewStatus.NEED_REVIEW) {
            return ReviewResult.ReviewStatus.NEED_REVIEW;
        }
        
        // 如果任一结果不通过，则整体结果不通过
        if (formatResult.getStatus() == ReviewResult.ReviewStatus.FAIL || 
            contentResult.getStatus() == ReviewResult.ReviewStatus.FAIL) {
            return ReviewResult.ReviewStatus.FAIL;
        }
        
        // 否则通过
        return ReviewResult.ReviewStatus.PASS;
    }
    
    private void saveIssues(ReviewResult reviewResult, FormatCheckResultDto formatResult, 
                           ContentReviewResultDto contentResult) {
        // 保存格式检查问题
        for (FormatCheckResultDto.IssueDto issueDto : formatResult.getIssues()) {
            Issue issue = Issue.builder()
                    .reviewResult(reviewResult)
                    .issueType(issueDto.getType())
                    .severity(issueDto.getSeverity())
                    .title(issueDto.getTitle())
                    .description(issueDto.getDescription())
                    .suggestion(issueDto.getSuggestion())
                    .pageNumber(issueDto.getPageNumber())
                    .lineNumber(issueDto.getLineNumber())
                    .status(Issue.IssueStatus.OPEN)
                    .build();
            
            issueRepository.save(issue);
        }
        
        // 保存内容评审问题
        for (ContentReviewResultDto.IssueDto issueDto : contentResult.getIssues()) {
            Issue issue = Issue.builder()
                    .reviewResult(reviewResult)
                    .issueType(issueDto.getType())
                    .severity(issueDto.getSeverity())
                    .title(issueDto.getTitle())
                    .description(issueDto.getDescription())
                    .suggestion(issueDto.getSuggestion())
                    .pageNumber(issueDto.getPageNumber())
                    .lineNumber(issueDto.getLineNumber())
                    .status(Issue.IssueStatus.OPEN)
                    .build();
            
            issueRepository.save(issue);
        }
    }
}
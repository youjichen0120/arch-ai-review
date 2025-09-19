package cn.hexinfo.arch_ai_review.service.impl;

import cn.hexinfo.arch_ai_review.dto.DocumentContentDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.service.DocumentService;
import cn.hexinfo.arch_ai_review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class AsyncDocumentProcessingService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ReviewService reviewService;

    /**
     * 异步处理文档：解析并评审
     * 
     * @param documentId 文档ID
     */
    @Async
    public void processDocument(Long documentId) {
        log.info("开始异步处理文档: {}", documentId);
        
        try {
            // 解析文档
            DocumentContentDto contentDto = documentService.getDocumentContent(documentId);
            log.info("文档解析完成: {}, 内容长度: {}", documentId, contentDto.getContent().length());
            
            // 开始评审
            documentService.updateDocumentStatus(documentId, Document.DocumentStatus.REVIEWING);
            
            // 执行评审
            reviewService.startReview(documentId);
            
            // 更新状态为已完成
            documentService.updateDocumentStatus(documentId, Document.DocumentStatus.COMPLETED);
            log.info("文档处理完成: {}", documentId);
            
        } catch (IOException e) {
            log.error("文档处理失败: {}", documentId, e);
            documentService.updateDocumentStatus(documentId, Document.DocumentStatus.FAILED);
        } catch (Exception e) {
            log.error("文档评审失败: {}", documentId, e);
            documentService.updateDocumentStatus(documentId, Document.DocumentStatus.FAILED);
        }
    }
}

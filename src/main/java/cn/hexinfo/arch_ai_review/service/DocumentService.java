package cn.hexinfo.arch_ai_review.service;

import cn.hexinfo.arch_ai_review.dto.DocumentContentDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentService {
    
    /**
     * 上传文档
     * 
     * @param file 文件
     * @param userId 用户ID
     * @return 文档实体
     * @throws IOException 如果上传过程中发生I/O错误
     */
    Document uploadDocument(MultipartFile file, Long userId) throws IOException;
    
    /**
     * 获取文档内容
     * 
     * @param documentId 文档ID
     * @return 文档内容DTO
     * @throws IOException 如果解析过程中发生I/O错误
     */
    DocumentContentDto getDocumentContent(Long documentId) throws IOException;
    
    /**
     * 获取用户的文档列表
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> getUserDocuments(Long userId, Pageable pageable);
    
    /**
     * 获取文档详情
     * 
     * @param documentId 文档ID
     * @return 文档实体
     */
    Document getDocument(Long documentId);
    
    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteDocument(Long documentId, Long userId);
    
    /**
     * 更新文档状态
     * 
     * @param documentId 文档ID
     * @param status 文档状态
     * @return 更新后的文档实体
     */
    Document updateDocumentStatus(Long documentId, Document.DocumentStatus status);
}

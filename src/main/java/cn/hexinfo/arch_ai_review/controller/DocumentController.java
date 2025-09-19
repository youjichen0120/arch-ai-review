package cn.hexinfo.arch_ai_review.controller;

import cn.hexinfo.arch_ai_review.dto.ApiResponse;
import cn.hexinfo.arch_ai_review.dto.DocumentResponseDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.entity.User;
import cn.hexinfo.arch_ai_review.service.AuthService;
import cn.hexinfo.arch_ai_review.service.DocumentService;
import cn.hexinfo.arch_ai_review.service.impl.AsyncDocumentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private AsyncDocumentProcessingService asyncDocumentProcessingService;
    
    @PostMapping("/upload")
    public ApiResponse<DocumentResponseDto> uploadDocument(@RequestParam("file") MultipartFile file) throws IOException {
        User currentUser = authService.getCurrentUser();
        Document document = documentService.uploadDocument(file, currentUser.getId());
        
        // 异步处理文档
        asyncDocumentProcessingService.processDocument(document.getId());
        
        return ApiResponse.success("文档上传成功", DocumentResponseDto.fromEntity(document));
    }
    
    @PostMapping("/{documentId}/process")
    public ApiResponse<String> processDocument(@PathVariable Long documentId) {
        Document document = documentService.getDocument(documentId);
        
        if (document.getStatus() == Document.DocumentStatus.UPLOADING) {
            // 开始异步处理
            asyncDocumentProcessingService.processDocument(documentId);
            return ApiResponse.success("文档处理已开始");
        } else {
            return ApiResponse.error(400, "文档状态不正确: " + document.getStatus());
        }
    }
    
    @GetMapping
    public ApiResponse<Page<DocumentResponseDto>> getUserDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Document> documents = documentService.getUserDocuments(currentUser.getId(), pageable);
        
        Page<DocumentResponseDto> responsePage = documents.map(DocumentResponseDto::fromEntity);
        return ApiResponse.success(responsePage);
    }
    
    @GetMapping("/{documentId}")
    public ApiResponse<DocumentResponseDto> getDocument(@PathVariable Long documentId) {
        Document document = documentService.getDocument(documentId);
        return ApiResponse.success(DocumentResponseDto.fromEntity(document));
    }
    
    @DeleteMapping("/{documentId}")
    public ApiResponse<String> deleteDocument(@PathVariable Long documentId) {
        User currentUser = authService.getCurrentUser();
        boolean result = documentService.deleteDocument(documentId, currentUser.getId());
        if (result) {
            return ApiResponse.success("文档删除成功");
        } else {
            return ApiResponse.error(500, "文档删除失败");
        }
    }
}

package cn.hexinfo.arch_ai_review.service.impl;

import cn.hexinfo.arch_ai_review.dto.DocumentContentDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.entity.User;
import cn.hexinfo.arch_ai_review.exception.ResourceNotFoundException;
import cn.hexinfo.arch_ai_review.exception.UnsupportedDocumentTypeException;
import cn.hexinfo.arch_ai_review.repository.DocumentRepository;
import cn.hexinfo.arch_ai_review.repository.UserRepository;
import cn.hexinfo.arch_ai_review.service.DocumentParserService;
import cn.hexinfo.arch_ai_review.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final String[] SUPPORTED_FILE_TYPES = {"docx", "pdf"};

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentParserFactory documentParserFactory;

    @Override
    @Transactional
    public Document uploadDocument(MultipartFile file, Long userId) throws IOException {
        log.info("开始上传文档: {}, 用户ID: {}", file.getOriginalFilename(), userId);
        
        // 验证用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userId));
        
        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        
        if (!isSupportedFileType(fileType)) {
            throw new UnsupportedDocumentTypeException("不支持的文件类型: " + fileType);
        }
        
        // 创建上传目录
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        
        // 生成唯一文件名
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileType;
        Path filePath = Paths.get(uploadDir, uniqueFilename);
        
        // 保存文件
        Files.copy(file.getInputStream(), filePath);
        
        // 创建文档记录
        Document document = Document.builder()
                .user(user)
                .filename(uniqueFilename)
                .originalFilename(originalFilename)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .fileType(fileType)
                .status(Document.DocumentStatus.UPLOADING)
                .build();
        
        document = documentRepository.save(document);
        
        log.info("文档上传成功: {}, ID: {}", originalFilename, document.getId());
        return document;
    }

    @Override
    public DocumentContentDto getDocumentContent(Long documentId) throws IOException {
        log.info("获取文档内容: {}", documentId);
        
        Document document = getDocument(documentId);
        
        // 更新文档状态为解析中
        document = updateDocumentStatus(documentId, Document.DocumentStatus.PARSING);
        
        try {
            // 获取对应的解析器
            DocumentParserService parser = documentParserFactory.getParser(document.getFileType());
            
            // 解析文档
            DocumentContentDto contentDto = parser.parseDocument(document);
            
            // 更新文档状态为已解析
            updateDocumentStatus(documentId, Document.DocumentStatus.PARSED);
            
            return contentDto;
        } catch (Exception e) {
            // 更新文档状态为失败
            updateDocumentStatus(documentId, Document.DocumentStatus.FAILED);
            log.error("解析文档失败: {}", documentId, e);
            throw e;
        }
    }

    @Override
    public Page<Document> getUserDocuments(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userId));
        
        return documentRepository.findByUser(user, pageable);
    }

    @Override
    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
    }

    @Override
    @Transactional
    public boolean deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.findByIdAndUser(documentId, 
                userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userId)))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在或无权限: " + documentId));
        
        try {
            // 删除物理文件
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
            
            // 删除数据库记录
            documentRepository.delete(document);
            
            return true;
        } catch (Exception e) {
            log.error("删除文档失败: {}", documentId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public Document updateDocumentStatus(Long documentId, Document.DocumentStatus status) {
        Document document = getDocument(documentId);
        document.setStatus(status);
        return documentRepository.save(document);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private boolean isSupportedFileType(String fileType) {
        return Arrays.asList(SUPPORTED_FILE_TYPES).contains(fileType.toLowerCase());
    }
}

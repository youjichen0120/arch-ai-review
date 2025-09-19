package cn.hexinfo.arch_ai_review.service;

import cn.hexinfo.arch_ai_review.dto.DocumentContentDto;
import cn.hexinfo.arch_ai_review.entity.Document;

import java.io.IOException;

public interface DocumentParserService {
    
    /**
     * 解析文档内容
     * 
     * @param document 文档实体
     * @return 文档内容DTO
     * @throws IOException 如果解析过程中发生I/O错误
     */
    DocumentContentDto parseDocument(Document document) throws IOException;
    
    /**
     * 检查文件类型是否支持
     * 
     * @param fileType 文件类型
     * @return 如果支持则返回true，否则返回false
     */
    boolean isSupportedFileType(String fileType);
}

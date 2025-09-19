package cn.hexinfo.arch_ai_review.service.impl;

import cn.hexinfo.arch_ai_review.exception.UnsupportedDocumentTypeException;
import cn.hexinfo.arch_ai_review.service.DocumentParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentParserFactory {
    
    private final List<DocumentParserService> documentParsers;
    
    @Autowired
    public DocumentParserFactory(List<DocumentParserService> documentParsers) {
        this.documentParsers = documentParsers;
    }
    
    /**
     * 根据文件类型获取对应的文档解析器
     * 
     * @param fileType 文件类型
     * @return 文档解析器
     * @throws UnsupportedDocumentTypeException 如果不支持该文件类型
     */
    public DocumentParserService getParser(String fileType) {
        for (DocumentParserService parser : documentParsers) {
            if (parser.isSupportedFileType(fileType)) {
                return parser;
            }
        }
        throw new UnsupportedDocumentTypeException("不支持的文档类型: " + fileType);
    }
}

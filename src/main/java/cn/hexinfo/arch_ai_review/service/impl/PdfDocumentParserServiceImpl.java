package cn.hexinfo.arch_ai_review.service.impl;

import cn.hexinfo.arch_ai_review.dto.DocumentContentDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfDocumentParserServiceImpl implements DocumentParserService {

    private static final Pattern TABLE_PATTERN = Pattern.compile("表\\s*(\\d+[-.]\\d+|\\d+)\\s*[:：]?\\s*(.+)");
    private static final Pattern FIGURE_PATTERN = Pattern.compile("图\\s*(\\d+[-.]\\d+|\\d+)\\s*[:：]?\\s*(.+)");
    private static final Pattern SECTION_PATTERN = Pattern.compile("^(\\d+(\\.\\d+)*)\\s+(.+)$|^第[一二三四五六七八九十百千万亿]+[章节]\\s+(.+)$");
    private static final String[] SUPPORTED_FILE_TYPES = {"pdf"};

    @Override
    public DocumentContentDto parseDocument(Document document) throws IOException {
        log.info("开始解析PDF文档: {}", document.getFilename());
        
        File file = new File(document.getFilePath());
        try (PDDocument pdfDocument = Loader.loadPDF(file)) {
            DocumentContentDto contentDto = DocumentContentDto.builder()
                    .documentId(document.getId())
                    .fileType(document.getFileType())
                    .build();
            
            // 提取文本内容
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(pdfDocument);
            contentDto.setContent(text);
            
            // 提取标题
            String title = extractTitle(pdfDocument, text);
            contentDto.setTitle(title);
            
            // 提取章节
            List<String> sections = extractSections(text);
            contentDto.setSections(sections);
            
            // 提取表格和图片引用
            List<DocumentContentDto.TableDto> tables = extractTables(text, pdfDocument.getNumberOfPages());
            List<DocumentContentDto.ImageDto> images = extractImages(text, pdfDocument.getNumberOfPages());
            
            contentDto.setTables(tables);
            contentDto.setImages(images);
            
            log.info("PDF文档解析完成: {}, 内容长度: {}", document.getFilename(), text.length());
            return contentDto;
            
        } catch (Exception e) {
            log.error("解析PDF文档失败: {}", document.getFilename(), e);
            throw new IOException("解析PDF文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isSupportedFileType(String fileType) {
        if (fileType == null) {
            return false;
        }
        
        for (String supportedType : SUPPORTED_FILE_TYPES) {
            if (supportedType.equalsIgnoreCase(fileType)) {
                return true;
            }
        }
        return false;
    }
    
    private String extractTitle(PDDocument pdfDocument, String text) {
        // 尝试从PDF文档信息中获取标题
        if (pdfDocument.getDocumentInformation() != null && 
            pdfDocument.getDocumentInformation().getTitle() != null && 
            !pdfDocument.getDocumentInformation().getTitle().trim().isEmpty()) {
            return pdfDocument.getDocumentInformation().getTitle();
        }
        
        // 如果文档信息中没有标题，则尝试从文本内容的前几行中提取
        String[] lines = text.split("\\r?\\n");
        if (lines.length > 0) {
            // 简单地取第一行非空文本作为标题
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            }
        }
        
        return "未命名文档";
    }
    
    private List<String> extractSections(String text) {
        List<String> sections = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                Matcher matcher = SECTION_PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    sections.add(line.trim());
                }
            }
        }
        
        return sections;
    }
    
    private List<DocumentContentDto.TableDto> extractTables(String text, int totalPages) {
        List<DocumentContentDto.TableDto> tables = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        
        int estimatedCurrentPage = 1;
        int lineCount = 0;
        int linesPerPage = estimateAverageLinesPerPage(text, totalPages);
        
        for (String line : lines) {
            lineCount++;
            if (lineCount > linesPerPage) {
                estimatedCurrentPage++;
                lineCount = 1;
            }
            
            if (line != null && !line.trim().isEmpty()) {
                Matcher matcher = TABLE_PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    DocumentContentDto.TableDto tableDto = new DocumentContentDto.TableDto();
                    tableDto.setCaption(line.trim());
                    tableDto.setReference(matcher.group(1));
                    tableDto.setPageNumber(estimatedCurrentPage);
                    tables.add(tableDto);
                }
            }
        }
        
        return tables;
    }
    
    private List<DocumentContentDto.ImageDto> extractImages(String text, int totalPages) {
        List<DocumentContentDto.ImageDto> images = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        
        int estimatedCurrentPage = 1;
        int lineCount = 0;
        int linesPerPage = estimateAverageLinesPerPage(text, totalPages);
        
        for (String line : lines) {
            lineCount++;
            if (lineCount > linesPerPage) {
                estimatedCurrentPage++;
                lineCount = 1;
            }
            
            if (line != null && !line.trim().isEmpty()) {
                Matcher matcher = FIGURE_PATTERN.matcher(line.trim());
                if (matcher.find()) {
                    DocumentContentDto.ImageDto imageDto = new DocumentContentDto.ImageDto();
                    imageDto.setCaption(line.trim());
                    imageDto.setReference(matcher.group(1));
                    imageDto.setPageNumber(estimatedCurrentPage);
                    images.add(imageDto);
                }
            }
        }
        
        return images;
    }
    
    private int estimateAverageLinesPerPage(String text, int totalPages) {
        if (totalPages <= 0) {
            return 50; // 默认值
        }
        
        String[] lines = text.split("\\r?\\n");
        return Math.max(1, lines.length / totalPages);
    }
}

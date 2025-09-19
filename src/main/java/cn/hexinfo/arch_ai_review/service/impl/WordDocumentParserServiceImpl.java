package cn.hexinfo.arch_ai_review.service.impl;

import cn.hexinfo.arch_ai_review.dto.DocumentContentDto;
import cn.hexinfo.arch_ai_review.entity.Document;
import cn.hexinfo.arch_ai_review.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class WordDocumentParserServiceImpl implements DocumentParserService {

    private static final Pattern TABLE_CAPTION_PATTERN = Pattern.compile("表\\s*(\\d+[-.]\\d+|\\d+)\\s*[:：]?\\s*(.+)");
    private static final Pattern FIGURE_CAPTION_PATTERN = Pattern.compile("图\\s*(\\d+[-.]\\d+|\\d+)\\s*[:：]?\\s*(.+)");
    private static final String[] SUPPORTED_FILE_TYPES = {"docx"};

    @Override
    public DocumentContentDto parseDocument(Document document) throws IOException {
        log.info("开始解析Word文档: {}", document.getFilename());
        
        try (FileInputStream fis = new FileInputStream(document.getFilePath())) {
            XWPFDocument doc = new XWPFDocument(fis);
            
            DocumentContentDto contentDto = DocumentContentDto.builder()
                    .documentId(document.getId())
                    .fileType(document.getFileType())
                    .build();
            
            StringBuilder contentBuilder = new StringBuilder();
            List<String> sections = new ArrayList<>();
            List<DocumentContentDto.TableDto> tables = new ArrayList<>();
            List<DocumentContentDto.ImageDto> images = new ArrayList<>();
            
            // 提取标题
            String title = extractTitle(doc);
            contentDto.setTitle(title);
            
            // 处理文档主体
            int pageNumber = 1; // 简化处理，实际页码可能需要更复杂的逻辑
            
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    String text = paragraph.getText();
                    
                    if (text != null && !text.trim().isEmpty()) {
                        contentBuilder.append(text).append("\n");
                        
                        // 检查是否为章节标题
                        if (isSection(paragraph)) {
                            sections.add(text);
                        }
                        
                        // 检查是否为图片标题
                        Matcher figureMatcher = FIGURE_CAPTION_PATTERN.matcher(text);
                        if (figureMatcher.find()) {
                            DocumentContentDto.ImageDto imageDto = DocumentContentDto.ImageDto.builder()
                                    .caption(text)
                                    .pageNumber(pageNumber)
                                    .reference(figureMatcher.group(1))
                                    .build();
                            images.add(imageDto);
                        }
                    }
                    
                    // 处理段落中的图片
                    for (XWPFRun run : paragraph.getRuns()) {
                        if (run.getEmbeddedPictures() != null && !run.getEmbeddedPictures().isEmpty()) {
                            DocumentContentDto.ImageDto imageDto = DocumentContentDto.ImageDto.builder()
                                    .caption("未命名图片")
                                    .pageNumber(pageNumber)
                                    .reference("未编号")
                                    .build();
                            images.add(imageDto);
                        }
                    }
                    
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    DocumentContentDto.TableDto tableDto = parseTable(table, pageNumber);
                    tables.add(tableDto);
                    
                    // 将表格内容也添加到文本中
                    contentBuilder.append("表格内容:\n");
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            contentBuilder.append(cell.getText()).append("\t");
                        }
                        contentBuilder.append("\n");
                    }
                    contentBuilder.append("\n");
                }
                
                // 简单的页码估算，实际应根据分页符等确定
                if (element instanceof XWPFParagraph && ((XWPFParagraph) element).isPageBreak()) {
                    pageNumber++;
                }
            }
            
            contentDto.setContent(contentBuilder.toString());
            contentDto.setSections(sections);
            contentDto.setTables(tables);
            contentDto.setImages(images);
            
            log.info("Word文档解析完成: {}, 内容长度: {}", document.getFilename(), contentBuilder.length());
            return contentDto;
            
        } catch (Exception e) {
            log.error("解析Word文档失败: {}", document.getFilename(), e);
            throw new IOException("解析Word文档失败: " + e.getMessage(), e);
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
    
    private String extractTitle(XWPFDocument doc) {
        // 尝试从文档属性中获取标题
        String title = doc.getProperties().getCoreProperties().getTitle();
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        
        // 如果文档属性中没有标题，则尝试从第一段获取
        List<XWPFParagraph> paragraphs = doc.getParagraphs();
        if (!paragraphs.isEmpty()) {
            XWPFParagraph firstParagraph = paragraphs.get(0);
            if (firstParagraph.getText() != null && !firstParagraph.getText().trim().isEmpty()) {
                return firstParagraph.getText().trim();
            }
        }
        
        return "未命名文档";
    }
    
    private boolean isSection(XWPFParagraph paragraph) {
        // 判断是否为章节标题的简单逻辑
        // 实际应用中可能需要更复杂的逻辑，如检查段落样式、编号等
        String text = paragraph.getText();
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否为常见的章节标题格式，如"1. 引言"，"第一章 引言"等
        return paragraph.getStyleID() != null && 
               (paragraph.getStyleID().toLowerCase().contains("heading") || 
                paragraph.getStyleID().toLowerCase().contains("title")) ||
               text.matches("^\\d+(\\.\\d+)*\\s+.+") || // 如 "1.1 系统架构"
               text.matches("^第[一二三四五六七八九十百千万亿]+[章节]\\s+.+"); // 如 "第一章 引言"
    }
    
    private DocumentContentDto.TableDto parseTable(XWPFTable table, int pageNumber) {
        DocumentContentDto.TableDto tableDto = new DocumentContentDto.TableDto();
        tableDto.setPageNumber(pageNumber);
        tableDto.setReference("未编号");
        tableDto.setCaption("未命名表格");
        
        // 尝试从表格前的段落中查找表格标题
        XWPFParagraph prevParagraph = findPreviousParagraph(table);
        if (prevParagraph != null) {
            String text = prevParagraph.getText();
            Matcher matcher = TABLE_CAPTION_PATTERN.matcher(text);
            if (matcher.find()) {
                tableDto.setReference(matcher.group(1));
                tableDto.setCaption(text);
            }
        }
        
        // 解析表格数据
        List<List<String>> data = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> rowData = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                rowData.add(cell.getText());
            }
            data.add(rowData);
        }
        tableDto.setData(data);
        
        return tableDto;
    }
    
    private XWPFParagraph findPreviousParagraph(XWPFTable table) {
        // 此方法在实际应用中需要更复杂的逻辑
        // 简化处理，返回null
        return null;
    }
}

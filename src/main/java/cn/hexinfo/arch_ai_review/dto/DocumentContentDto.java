package cn.hexinfo.arch_ai_review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContentDto {
    
    private String title;
    private String content;
    private String fileType;
    private Long documentId;
    private List<String> sections = new ArrayList<>();
    private List<ImageDto> images = new ArrayList<>();
    private List<TableDto> tables = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDto {
        private String caption;
        private Integer pageNumber;
        private String reference;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableDto {
        private String caption;
        private Integer pageNumber;
        private String reference;
        private List<List<String>> data = new ArrayList<>();
    }
}

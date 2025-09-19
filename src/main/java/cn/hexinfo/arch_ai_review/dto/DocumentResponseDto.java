package cn.hexinfo.arch_ai_review.dto;

import cn.hexinfo.arch_ai_review.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {
    
    private Long documentId;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String fileType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static DocumentResponseDto fromEntity(Document document) {
        return DocumentResponseDto.builder()
                .documentId(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .status(document.getStatus().name())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}

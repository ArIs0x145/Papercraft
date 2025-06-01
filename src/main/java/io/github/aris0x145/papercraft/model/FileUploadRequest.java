package io.github.aris0x145.papercraft.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上傳請求模型
 */
@Data
public class FileUploadRequest {
    
    /**
     * 上傳的文件列表
     */
    private List<MultipartFile> files;
    
    /**
     * 報告生成需求
     */
    private String reportTitle;
    
    /**
     * 報告類型 (如：summary, analysis, integration)
     */
    private String reportType;
    
    /**
     * 特殊要求或指示
     */
    private String instructions;
    
    /**
     * 預期報告長度
     */
    private Integer expectedLength;
}

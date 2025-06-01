package io.github.aris0x145.papercraft.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件處理結果模型
 */
@Data
public class FileProcessingResult {
    
    /**
     * 處理成功的文件信息
     */
    private List<ProcessedFile> processedFiles;
    
    /**
     * 處理失敗的文件信息
     */
    private List<FailedFile> failedFiles;
    
    /**
     * 提取的總內容摘要
     */
    private String contentSummary;
    
    /**
     * 處理時間
     */
    private LocalDateTime processedAt;
    
    /**
     * 總文檔數量
     */
    private int totalDocuments;
    
    /**
     * 總內容長度（字符數）
     */
    private long totalContentLength;
    
    /**
     * 處理後的文檔內容（用於傳遞給 Agent）
     */
    private String combinedContent;
      @Data
    public static class ProcessedFile {
        private String fileName;
        private String fileType;
        private long fileSize;
        private int documentCount;
        private long contentLength;
        private String content;  // 文件的實際內容
        private Map<String, Object> metadata;
    }
    
    @Data
    public static class FailedFile {
        private String fileName;
        private String errorMessage;
        private String fileType;
    }
}

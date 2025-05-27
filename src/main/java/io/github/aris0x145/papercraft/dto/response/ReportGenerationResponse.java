package io.github.aris0x145.papercraft.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 報告生成響應 DTO
 */
@Data
@Builder
public class ReportGenerationResponse {

    /**
     * 報告 ID
     */
    private String reportId;

    /**
     * 報告標題
     */
    private String title;

    /**
     * 報告內容
     */
    private String content;

    /**
     * 報告類型
     */
    private String reportType;

    /**
     * 語言
     */
    private String language;

    /**
     * 報告結構
     */
    private ReportStructure structure;

    /**
     * 生成時間
     */
    private LocalDateTime generatedTime;

    /**
     * 使用的文檔 ID 列表
     */
    private List<String> sourceDocumentIds;

    /**
     * 參考文獻
     */
    private List<Reference> references;

    /**
     * 生成統計資訊
     */
    private GenerationMetrics metrics;

    /**
     * 報告結構
     */
    @Data
    @Builder
    public static class ReportStructure {
        private String abstractSection;
        private String introduction;
        private String methodology;
        private String results;
        private String discussion;
        private String conclusion;
        private Map<String, String> customSections;
    }

    /**
     * 參考文獻
     */
    @Data
    @Builder
    public static class Reference {
        private String title;
        private String authors;
        private String source;
        private String url;
        private LocalDateTime accessDate;
    }

    /**
     * 生成統計資訊
     */
    @Data
    @Builder
    public static class GenerationMetrics {
        private Integer wordCount;
        private Integer characterCount;
        private Integer sectionsCount;
        private Integer referencesCount;
        private Long processingTimeMs;
        private Integer tokensUsed;
    }
}

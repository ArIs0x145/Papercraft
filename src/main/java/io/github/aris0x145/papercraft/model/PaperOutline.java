package io.github.aris0x145.papercraft.model;

import lombok.Data;

import java.util.List;

/**
 * 論文大綱 - Spring AI 結構化輸出
 */
@Data
public class PaperOutline {
    
    /**
     * 論文標題
     */
    private String title;
    
    /**
     * 摘要
     */
    private String abstractText;
    
    /**
     * 關鍵詞
     */
    private List<String> keywords;
    
    /**
     * 章節列表
     */
    private List<Section> sections;
    
    @Data
    public static class Section {
        /**
         * 章節標題
         */
        private String title;
        
        /**
         * 章節描述
         */
        private String description;
        
        /**
         * 預估字數
         */
        private Integer estimatedWords;
        
        /**
         * 子章節
         */
        private List<String> subsections;
    }
}

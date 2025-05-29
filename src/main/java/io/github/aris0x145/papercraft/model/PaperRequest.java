package io.github.aris0x145.papercraft.model;

import lombok.Data;

/**
 * 論文生成請求
 */
@Data
public class PaperRequest {
    
    /**
     * 論文主題
     */
    private String topic;
    
    /**
     * 論文類型 (research, review, survey, etc.)
     */
    private String type = "research";
    
    /**
     * 目標字數
     */
    private Integer wordCount = 3000;
    
    /**
     * 學術領域
     */
    private String field;
    
    /**
     * 特殊要求
     */
    private String requirements;
}

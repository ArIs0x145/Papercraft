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
     * 論文類型
     */
    private String type;
    
    /**
     * 學術領域
     */
    private String field;
      /**
     * 輸出語言
     */
    private String language;
    
    /**
     * 特殊需求
     */
    private String requirements;
}

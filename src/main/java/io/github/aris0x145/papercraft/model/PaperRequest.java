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
     * 論文類型 (用戶自定義，如：研究論文、文獻綜述、技術報告等)
     */
    private String type;
    
    /**
     * 學術領域 (用戶自定義，如：計算機科學、生物學、經濟學等)
     */
    private String field;
      /**
     * 輸出語言 (如：繁體中文、English、简体中文等)
     */
    private String language;
    
    /**
     * 特殊需求 (用戶自定義要求，如：字數要求、引用格式、特定內容等)
     */
    private String requirements;
}

package io.github.aris0x145.papercraft.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 論文生成結果
 */
@Data
public class PaperResult {
    
    /**
     * 生成ID
     */
    private String id;
    
    /**
     * 論文標題
     */
    private String title;
    
    /**
     * 完整論文內容
     */
    private String content;
    
    /**
     * 論文大綱
     */
    private PaperOutline outline;
    
    /**
     * 生成狀態
     */
    private GenerationStatus status;
    
    /**
     * 進度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 各章節內容 (章節標題 -> 內容)
     */
    private Map<String, String> sectionContents;
    
    /**
     * 創建時間
     */
    private LocalDateTime createdAt;
    
    /**
     * 完成時間
     */
    private LocalDateTime completedAt;
    
    /**
     * 錯誤訊息 (如果有)
     */
    private String errorMessage;
    
    public enum GenerationStatus {
        PLANNING,     // 正在規劃大綱
        WRITING,      // 正在撰寫內容
        EDITING,      // 正在編輯潤色
        COMPLETED,    // 已完成
        FAILED        // 生成失敗
    }
}

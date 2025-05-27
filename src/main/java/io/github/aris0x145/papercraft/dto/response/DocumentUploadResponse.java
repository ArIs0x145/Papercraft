package io.github.aris0x145.papercraft.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文檔上傳響應 DTO
 */
@Data
@Builder
public class DocumentUploadResponse {

    /**
     * 文檔 ID
     */
    private String documentId;

    /**
     * 原始檔案名稱
     */
    private String originalFileName;

    /**
     * 檔案大小（bytes）
     */
    private Long fileSize;

    /**
     * 檔案類型
     */
    private String fileType;

    /**
     * 處理狀態
     */
    private ProcessingStatus status;

    /**
     * 上傳時間
     */
    private LocalDateTime uploadTime;

    /**
     * 文檔摘要
     */
    private String summary;

    /**
     * 關鍵詞
     */
    private List<String> keywords;

    /**
     * 處理的頁面數或章節數
     */
    private Integer processedUnits;

    /**
     * 錯誤訊息（如果處理失敗）
     */
    private String errorMessage;

    /**
     * 處理狀態枚舉
     */
    public enum ProcessingStatus {
        UPLOADED,       // 已上傳
        PROCESSING,     // 處理中
        COMPLETED,      // 處理完成
        FAILED          // 處理失敗
    }
}

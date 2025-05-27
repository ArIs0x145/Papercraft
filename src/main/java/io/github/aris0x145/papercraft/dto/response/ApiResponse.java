package io.github.aris0x145.papercraft.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 統一 API 響應包裝類
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 響應狀態碼
     */
    private Integer code;

    /**
     * 響應訊息
     */
    private String message;

    /**
     * 響應數據
     */
    private T data;

    /**
     * 響應時間戳
     */
    private LocalDateTime timestamp;

    /**
     * 請求追蹤 ID
     */
    private String traceId;

    /**
     * 成功響應的靜態工廠方法
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data, LocalDateTime.now(), generateTraceId());
    }

    /**
     * 成功響應的靜態工廠方法（自定義訊息）
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, message, data, LocalDateTime.now(), generateTraceId());
    }

    /**
     * 失敗響應的靜態工廠方法
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now(), generateTraceId());
    }

    /**
     * 失敗響應的靜態工廠方法（預設錯誤碼）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, LocalDateTime.now(), generateTraceId());
    }

    /**
     * 生成追蹤 ID
     */
    private static String generateTraceId() {
        return "papercraft-" + System.currentTimeMillis() + "-" + 
               Long.toHexString(Double.doubleToLongBits(Math.random()));
    }
}

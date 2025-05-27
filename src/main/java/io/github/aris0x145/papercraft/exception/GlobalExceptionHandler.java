package io.github.aris0x145.papercraft.exception;

import io.github.aris0x145.papercraft.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局異常處理器
 * 統一處理應用程序中的異常並返回標準化的錯誤響應
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 處理參數驗證異常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("參數驗證失敗: {}", errorMessage);
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "參數驗證失敗: " + errorMessage));
    }

    /**
     * 處理檔案大小超限異常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeException(MaxUploadSizeExceededException e) {
        log.warn("檔案大小超過限制: {}", e.getMessage());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "檔案大小超過限制，請上傳小於 50MB 的檔案"));
    }

    /**
     * 處理參數異常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("參數錯誤: {}", e.getMessage());
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
    }

    /**
     * 處理 Spring AI 相關異常
     */
    @ExceptionHandler(org.springframework.ai.chat.client.ChatClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleChatClientException(Exception e) {
        log.error("AI 服務異常: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "AI 服務暫時不可用，請稍後再試"));
    }

    /**
     * 處理通用 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        log.error("運行時異常: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "系統內部錯誤"));
    }

    /**
     * 處理其他未捕獲的異常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("未知異常: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "系統發生未知錯誤"));
    }
}

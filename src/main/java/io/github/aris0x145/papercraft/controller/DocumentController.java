package io.github.aris0x145.papercraft.controller;

import io.github.aris0x145.papercraft.dto.response.ApiResponse;
import io.github.aris0x145.papercraft.dto.response.DocumentUploadResponse;
import io.github.aris0x145.papercraft.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文檔管理控制器
 * 負責文檔的上傳、處理和管理
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;

    /**
     * 上傳並處理文檔
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        log.info("接收到文檔上傳請求: {}", file.getOriginalFilename());

        try {
            DocumentUploadResponse response = documentProcessingService.processDocument(file);
            
            if (response.getStatus() == DocumentUploadResponse.ProcessingStatus.COMPLETED) {
                return ApiResponse.success(response, "文檔上傳並處理成功");
            } else {
                return ApiResponse.error(400, "文檔處理失敗: " + response.getErrorMessage());
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("文檔上傳驗證失敗: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("文檔上傳處理異常", e);
            return ApiResponse.error(500, "文檔處理時發生系統錯誤");
        }
    }

    /**
     * 健康檢查端點
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("文檔服務運行正常");
    }
}

package io.github.aris0x145.papercraft.controller;

import io.github.aris0x145.papercraft.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系統狀態控制器
 * 提供系統健康檢查和狀態監控功能
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Slf4j
public class SystemController {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    /**
     * 應用程序健康檢查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "application", "Papercraft",
            "version", "0.0.1-SNAPSHOT"
        );
        
        return ApiResponse.success(healthInfo, "系統運行正常");
    }

    /**
     * AI 服務狀態檢查
     */
    @GetMapping("/ai-status")
    public ApiResponse<Map<String, Object>> aiStatus() {
        try {
            // 簡單測試 ChatModel 連接
            String testResponse = chatModel.call("Hello");
            boolean chatModelHealthy = testResponse != null && !testResponse.trim().isEmpty();

            // 測試 EmbeddingModel
            boolean embeddingModelHealthy = true;
            try {
                embeddingModel.embed("test");
            } catch (Exception e) {
                embeddingModelHealthy = false;
                log.warn("Embedding model health check failed: {}", e.getMessage());
            }

            Map<String, Object> aiStatus = Map.of(
                "chatModel", chatModelHealthy ? "UP" : "DOWN",
                "embeddingModel", embeddingModelHealthy ? "UP" : "DOWN",
                "vectorStore", "UP", // SimpleVectorStore 總是可用
                "timestamp", LocalDateTime.now()
            );

            return ApiResponse.success(aiStatus, "AI 服務狀態檢查完成");

        } catch (Exception e) {
            log.error("AI 服務狀態檢查失敗", e);
            return ApiResponse.error(503, "AI 服務不可用: " + e.getMessage());
        }
    }

    /**
     * 系統資訊
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> systemInfo() {
        Map<String, Object> systemInfo = Map.of(
            "application", Map.of(
                "name", "Papercraft",
                "description", "論文報告 AI Agent",
                "version", "0.0.1-SNAPSHOT"
            ),
            "java", Map.of(
                "version", System.getProperty("java.version"),
                "vendor", System.getProperty("java.vendor")
            ),
            "server", Map.of(
                "timestamp", LocalDateTime.now(),
                "timezone", System.getProperty("user.timezone")
            ),
            "features", Map.of(
                "documentUpload", true,
                "pdfProcessing", true,
                "reportGeneration", true,
                "ragSupport", true,
                "multiLanguage", true
            )
        );

        return ApiResponse.success(systemInfo, "系統資訊獲取成功");
    }
}

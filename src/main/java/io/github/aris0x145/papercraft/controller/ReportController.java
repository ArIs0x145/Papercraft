package io.github.aris0x145.papercraft.controller;

import io.github.aris0x145.papercraft.dto.request.GenerateReportRequest;
import io.github.aris0x145.papercraft.dto.response.ApiResponse;
import io.github.aris0x145.papercraft.dto.response.ReportGenerationResponse;
import io.github.aris0x145.papercraft.service.ReportGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 報告生成控制器
 * 負責處理報告生成相關的 API 請求
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportGenerationService reportGenerationService;

    /**
     * 生成報告
     */
    @PostMapping("/generate")
    public ApiResponse<ReportGenerationResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request) {
        
        log.info("接收到報告生成請求: 主題={}, 類型={}", request.getTopic(), request.getReportType());

        try {
            ReportGenerationResponse response = reportGenerationService.generateReport(request);
            return ApiResponse.success(response, "報告生成成功");
            
        } catch (IllegalArgumentException e) {
            log.warn("報告生成請求驗證失敗: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("報告生成異常", e);
            return ApiResponse.error(500, "報告生成時發生系統錯誤: " + e.getMessage());
        }
    }

    /**
     * 健康檢查端點
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("報告服務運行正常");
    }
}

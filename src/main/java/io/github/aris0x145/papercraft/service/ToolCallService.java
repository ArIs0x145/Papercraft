package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.tools.CitationFormatterTool;
import io.github.aris0x145.papercraft.tools.DataAnalyzerTool;
import io.github.aris0x145.papercraft.tools.OutlineGeneratorTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工具調用服務
 * 整合所有 MCP 風格工具，提供統一的工具調用接口
 */
@Service
public class ToolCallService {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallService.class);

    private final DataAnalyzerTool dataAnalyzerTool;
    private final CitationFormatterTool citationFormatterTool;
    private final OutlineGeneratorTool outlineGeneratorTool;

    public ToolCallService(
            DataAnalyzerTool dataAnalyzerTool,
            CitationFormatterTool citationFormatterTool,
            OutlineGeneratorTool outlineGeneratorTool) {
        this.dataAnalyzerTool = dataAnalyzerTool;
        this.citationFormatterTool = citationFormatterTool;
        this.outlineGeneratorTool = outlineGeneratorTool;
    }

    /**
     * 分析數據工具調用
     */
    public CompletableFuture<DataAnalyzerTool.DataAnalysisResult> analyzeData(
            String data, String analysisType) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("調用數據分析工具 - 類型: {}", analysisType);
            return dataAnalyzerTool.analyzeData(data, analysisType);
        });
    }

    /**
     * 引用格式化工具調用
     */
    public CompletableFuture<CitationFormatterTool.CitationResult> formatCitation(
            String citation, String sourceType, String style) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("調用引用格式化工具 - 樣式: {}", style);
            return citationFormatterTool.formatCitation(citation, sourceType, style);
        });
    }

    /**
     * 大綱生成工具調用
     */
    public CompletableFuture<OutlineGeneratorTool.OutlineGenerationResult> generateOutline(
            String topic, String paperType, String requirements, Integer targetLength) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("調用大綱生成工具 - 主題: {}, 類型: {}", topic, paperType);
            return outlineGeneratorTool.generateOutline(topic, paperType, requirements, targetLength);
        });
    }

    /**
     * 研究建議工具調用
     */
    public CompletableFuture<OutlineGeneratorTool.ResearchAdviceResult> generateResearchAdvice(
            String topic, String researchScope) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("調用研究建議工具 - 主題: {}", topic);
            return outlineGeneratorTool.generateResearchAdvice(topic, researchScope);
        });
    }

    /**
     * 批量處理多個工具調用
     */
    public CompletableFuture<ToolCallBatchResult> executeBatchToolCalls(ToolCallBatchRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("執行批量工具調用 - {} 個任務", request.toolCalls().size());

            List<CompletableFuture<ToolCallResult>> futures = request.toolCalls().stream()
                    .map(this::executeToolCall)
                    .toList();

            List<ToolCallResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            return new ToolCallBatchResult(
                    results.stream().allMatch(r -> r.success()),
                    results,
                    System.currentTimeMillis()
            );
        });
    }

    /**
     * 執行單個工具調用
     */
    private CompletableFuture<ToolCallResult> executeToolCall(ToolCallRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Object result = switch (request.toolName()) {
                    case "data_analyzer" -> dataAnalyzerTool.analyzeData(
                            (String) request.parameters().get("data"),
                            (String) request.parameters().get("analysisType")
                    );
                    case "citation_formatter" -> citationFormatterTool.formatCitation(
                            (String) request.parameters().get("citation"),
                            (String) request.parameters().get("sourceType"),
                            (String) request.parameters().get("style")
                    );
                    case "outline_generator" -> outlineGeneratorTool.generateOutline(
                            (String) request.parameters().get("topic"),
                            (String) request.parameters().get("paperType"),
                            (String) request.parameters().get("requirements"),
                            (Integer) request.parameters().get("targetLength")
                    );
                    case "research_advisor" -> outlineGeneratorTool.generateResearchAdvice(
                            (String) request.parameters().get("topic"),
                            (String) request.parameters().get("researchScope")
                    );
                    default -> throw new IllegalArgumentException("未知的工具: " + request.toolName());
                };

                return new ToolCallResult(
                        true,
                        request.toolName(),
                        "工具調用成功",
                        result
                );

            } catch (Exception e) {
                logger.error("工具調用失敗: {} - {}", request.toolName(), e.getMessage(), e);
                return new ToolCallResult(
                        false,
                        request.toolName(),
                        "工具調用失敗: " + e.getMessage(),
                        null
                );
            }
        });
    }

    /**
     * 獲取可用工具列表
     */
    public List<ToolInfo> getAvailableTools() {
        return List.of(
                new ToolInfo(
                        "data_analyzer",
                        "數據分析工具",
                        "分析實驗數據、統計數據和文本數據",
                        List.of("data", "analysisType"),
                        "DataAnalysisResult"
                ),
                new ToolInfo(
                        "citation_formatter",
                        "引用格式化工具",
                        "格式化學術引用和參考文獻",
                        List.of("citation", "sourceType", "style"),
                        "CitationResult"
                ),
                new ToolInfo(
                        "outline_generator",
                        "大綱生成工具",
                        "生成結構化的論文或報告大綱",
                        List.of("topic", "paperType", "requirements", "targetLength"),
                        "OutlineGenerationResult"
                ),
                new ToolInfo(
                        "research_advisor",
                        "研究建議工具",
                        "提供研究方向和方法論建議",
                        List.of("topic", "researchScope"),
                        "ResearchAdviceResult"
                )
        );
    }

    /**
     * 驗證工具調用請求
     */
    public boolean validateToolCallRequest(ToolCallRequest request) {
        if (request == null || request.toolName() == null) {
            return false;
        }

        return getAvailableTools().stream()
                .anyMatch(tool -> tool.name().equals(request.toolName()));
    }

    /**
     * 工具信息記錄
     */
    public record ToolInfo(
            String name,
            String displayName,
            String description,
            List<String> parameters,
            String returnType
    ) {}

    /**
     * 工具調用請求記錄
     */
    public record ToolCallRequest(
            String toolName,
            Map<String, Object> parameters
    ) {}

    /**
     * 工具調用結果記錄
     */
    public record ToolCallResult(
            boolean success,
            String toolName,
            String message,
            Object result
    ) {}

    /**
     * 批量工具調用請求記錄
     */
    public record ToolCallBatchRequest(
            List<ToolCallRequest> toolCalls
    ) {}

    /**
     * 批量工具調用結果記錄
     */
    public record ToolCallBatchResult(
            boolean allSuccess,
            List<ToolCallResult> results,
            long timestamp
    ) {}
}

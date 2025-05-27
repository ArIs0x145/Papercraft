package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.dto.request.GenerateReportRequest;
import io.github.aris0x145.papercraft.dto.response.ReportGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 報告生成服務
 * 負責根據用戶需求和文檔內容生成學術報告
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    @Qualifier("mainChatClient")
    private final ChatClient mainChatClient;

    /**
     * 生成報告
     */
    public ReportGenerationResponse generateReport(GenerateReportRequest request) {
        String reportId = UUID.randomUUID().toString();
        log.info("開始生成報告: {} (ID: {})", request.getTopic(), reportId);

        long startTime = System.currentTimeMillis();

        try {
            // 構建報告生成提示詞
            String reportPrompt = buildReportPrompt(request);

            // 調用 ChatClient 生成報告（使用 RAG）
            String reportContent = mainChatClient.prompt()
                    .user(reportPrompt)
                    .call()
                    .content();

            long processingTime = System.currentTimeMillis() - startTime;

            // 解析報告結構
            ReportGenerationResponse.ReportStructure structure = parseReportStructure(reportContent);

            // 構建響應
            return ReportGenerationResponse.builder()
                    .reportId(reportId)
                    .title(generateReportTitle(request))
                    .content(reportContent)
                    .reportType(request.getReportType())
                    .language(request.getLanguage())
                    .structure(structure)
                    .generatedTime(LocalDateTime.now())
                    .sourceDocumentIds(getSourceDocumentIds())
                    .metrics(ReportGenerationResponse.GenerationMetrics.builder()
                            .wordCount(countWords(reportContent))
                            .characterCount(reportContent.length())
                            .sectionsCount(countSections(structure))
                            .processingTimeMs(processingTime)
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("生成報告時發生錯誤: {}", e.getMessage(), e);
            throw new RuntimeException("報告生成失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 構建報告生成提示詞
     */
    private String buildReportPrompt(GenerateReportRequest request) {
        StringBuilder promptBuilder = new StringBuilder();

        // 基本要求
        promptBuilder.append("請根據提供的文獻資料，撰寫一份關於「").append(request.getTopic()).append("」的");

        // 報告類型
        switch (request.getReportType()) {
            case "academic" -> promptBuilder.append("學術研究報告");
            case "literature_review" -> promptBuilder.append("文獻綜述報告");
            case "experiment" -> promptBuilder.append("實驗報告");
            case "custom" -> promptBuilder.append("專題報告");
        }

        promptBuilder.append("。\n\n");

        // 報告結構要求
        promptBuilder.append(getStructureTemplate(request.getReportType()));

        // 長度要求
        promptBuilder.append("\n\n報告長度要求：");
        switch (request.getLength()) {
            case "short" -> promptBuilder.append("簡潔版本（1000-2000字）");
            case "medium" -> promptBuilder.append("標準版本（2000-5000字）");
            case "long" -> promptBuilder.append("詳細版本（5000字以上）");
        }

        // 語言要求
        if ("zh-tw".equals(request.getLanguage())) {
            promptBuilder.append("\n\n請使用繁體中文撰寫，並遵循台灣學術寫作規範。");
        } else if ("zh-cn".equals(request.getLanguage())) {
            promptBuilder.append("\n\n請使用簡體中文撰寫，並遵循大陸學術寫作規範。");
        } else if ("en".equals(request.getLanguage())) {
            promptBuilder.append("\n\nPlease write in English following academic writing standards.");
        }

        // 參考文獻要求
        if (Boolean.TRUE.equals(request.getIncludeReferences())) {
            promptBuilder.append("\n\n請在報告最後包含參考文獻列表，並在內文中適當引用。");
        }

        // 自定義要求
        if (request.getCustomRequirements() != null && !request.getCustomRequirements().trim().isEmpty()) {
            promptBuilder.append("\n\n特殊要求：").append(request.getCustomRequirements());
        }

        // 品質要求
        promptBuilder.append("""
                
                
                品質要求：
                1. 邏輯清晰，結構完整
                2. 論述嚴謹，有理有據
                3. 語言專業，表達準確
                4. 內容充實，深度適當
                5. 格式規範，易於閱讀
                
                請基於向量資料庫中的相關文獻資料進行撰寫，確保內容的準確性和權威性。
                """);

        return promptBuilder.toString();
    }

    /**
     * 獲取報告結構模板
     */
    private String getStructureTemplate(String reportType) {
        return switch (reportType) {
            case "academic" -> """
                報告結構：
                1. 摘要 (Abstract)
                2. 前言 (Introduction)
                3. 文獻回顧 (Literature Review)
                4. 研究方法 (Methodology)
                5. 結果與分析 (Results and Analysis)
                6. 討論 (Discussion)
                7. 結論 (Conclusion)
                8. 參考文獻 (References)
                """;
            case "literature_review" -> """
                報告結構：
                1. 摘要 (Abstract)
                2. 引言 (Introduction)
                3. 主題分類與討論 (Thematic Analysis)
                4. 研究趨勢與發展 (Trends and Developments)
                5. 研究空白與建議 (Research Gaps and Recommendations)
                6. 結論 (Conclusion)
                7. 參考文獻 (References)
                """;
            case "experiment" -> """
                報告結構：
                1. 摘要 (Abstract)
                2. 實驗目的 (Objectives)
                3. 實驗方法 (Methods)
                4. 實驗設計 (Experimental Design)
                5. 結果 (Results)
                6. 分析與討論 (Analysis and Discussion)
                7. 結論 (Conclusion)
                8. 參考文獻 (References)
                """;
            default -> """
                報告結構：
                1. 摘要
                2. 前言
                3. 主要內容
                4. 分析討論
                5. 結論
                6. 參考文獻
                """;
        };
    }

    /**
     * 生成報告標題
     */
    private String generateReportTitle(GenerateReportRequest request) {
        String typePrefix = switch (request.getReportType()) {
            case "academic" -> "學術研究報告";
            case "literature_review" -> "文獻綜述";
            case "experiment" -> "實驗報告";
            default -> "研究報告";
        };
        return typePrefix + "：" + request.getTopic();
    }

    /**
     * 解析報告結構（簡化版本）
     */
    private ReportGenerationResponse.ReportStructure parseReportStructure(String content) {
        // 這裡可以實現更複雜的結構解析邏輯
        // 目前返回基本結構
        return ReportGenerationResponse.ReportStructure.builder()
                .abstractSection(extractSection(content, "摘要"))
                .introduction(extractSection(content, "前言|引言"))
                .methodology(extractSection(content, "方法|研究方法"))
                .results(extractSection(content, "結果|研究結果"))
                .discussion(extractSection(content, "討論|分析"))
                .conclusion(extractSection(content, "結論"))
                .customSections(Map.of())
                .build();
    }

    /**
     * 提取報告章節（簡化實現）
     */
    private String extractSection(String content, String sectionPattern) {
        // 這裡可以實現更精確的章節提取邏輯
        // 目前返回空值，實際使用時可以改進
        return null;
    }

    /**
     * 計算字數
     */
    private Integer countWords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        // 簡化的中英文字數計算
        return content.replaceAll("\\s+", " ").trim().split("\\s+").length;
    }

    /**
     * 計算章節數
     */
    private Integer countSections(ReportGenerationResponse.ReportStructure structure) {
        int count = 0;
        if (structure.getAbstractSection() != null) count++;
        if (structure.getIntroduction() != null) count++;
        if (structure.getMethodology() != null) count++;
        if (structure.getResults() != null) count++;
        if (structure.getDiscussion() != null) count++;
        if (structure.getConclusion() != null) count++;
        if (structure.getCustomSections() != null) count += structure.getCustomSections().size();
        return count;
    }

    /**
     * 獲取來源文檔 ID（簡化實現）
     */
    private java.util.List<String> getSourceDocumentIds() {
        // 實際實現中，可以從 RAG 上下文中獲取使用的文檔 ID
        return java.util.List.of();
    }
}

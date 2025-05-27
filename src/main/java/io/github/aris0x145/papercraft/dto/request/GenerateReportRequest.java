package io.github.aris0x145.papercraft.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

/**
 * 生成報告請求 DTO
 */
@Data
@Builder
public class GenerateReportRequest {

    /**
     * 報告主題
     */
    @NotBlank(message = "報告主題不能為空")
    private String topic;

    /**
     * 報告類型
     * academic: 學術研究報告
     * literature_review: 文獻綜述
     * experiment: 實驗報告
     * custom: 自定義
     */
    @Pattern(regexp = "^(academic|literature_review|experiment|custom)$", 
             message = "報告類型必須是 academic、literature_review、experiment 或 custom")
    private String reportType;

    /**
     * 語言設定
     */
    @Pattern(regexp = "^(zh-tw|zh-cn|en)$", 
             message = "語言設定必須是 zh-tw、zh-cn 或 en")
    @Builder.Default
    private String language = "zh-tw";

    /**
     * 報告長度
     * short: 簡短 (1000-2000字)
     * medium: 中等 (2000-5000字)  
     * long: 詳細 (5000字以上)
     */
    @Pattern(regexp = "^(short|medium|long)$", 
             message = "報告長度必須是 short、medium 或 long")
    @Builder.Default
    private String length = "medium";

    /**
     * 是否包含參考文獻
     */
    @Builder.Default
    private Boolean includeReferences = true;

    /**
     * 自定義要求或提示詞
     */
    private String customRequirements;

    /**
     * 報告結構模板
     */
    private String structureTemplate;
}

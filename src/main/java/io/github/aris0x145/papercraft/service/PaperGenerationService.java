package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.PaperRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * 論文生成服務
 */
@Slf4j
@Service
public class PaperGenerationService {

    private final ChatClient plannerAgent;
    private final ChatClient writerAgent;
    private final ChatClient editorAgent;

    /**
     * 構造函數 - 注入三個 Agent 的 ChatClient
     */
    public PaperGenerationService(
            @Qualifier("plannerAgentChatClient") ChatClient plannerAgent,
            @Qualifier("writerAgentChatClient") ChatClient writerAgent,
            @Qualifier("editorAgentChatClient") ChatClient editorAgent) {
        this.plannerAgent = plannerAgent;
        this.writerAgent = writerAgent;
        this.editorAgent = editorAgent;
    }

    /**
     * 流式生成論文
     * 使用 CONVERSATION_ID 參數實現多 Agent 間的記憶體共享
     */
    public Flux<String> generatePaperStream(PaperRequest request) {
        return generatePaperStream(request, null);
    }    /**
     * 流式生成論文（支援文件內容輸入）
     * 使用 CONVERSATION_ID 參數實現多 Agent 間的記憶體共享
     * 
     * @param request 論文生成請求
     * @param fileContents 上傳文件的內容列表（可選）
     */
    public Flux<String> generatePaperStream(PaperRequest request, List<String> fileContents) {
        String conversationId = "paper-" + UUID.randomUUID();
        log.info("Starting paper generation with conversationId: {}", conversationId);
        
        return Flux.concat(
            Flux.just("=== 開始生成論文大綱 ===\n\n")
                .concatWith(generateOutlineStream(request, fileContents, conversationId))
                .concatWith(Flux.just("\n\n=== 大綱生成完成 ===\n\n")),

            Flux.just("=== 開始撰寫論文內容 ===\n\n")
                .concatWith(writeContentStream(request, fileContents, conversationId))
                .concatWith(Flux.just("\n\n=== 內容撰寫完成 ===\n\n")),

            Flux.just("=== 開始編輯潤色 ===\n\n")
                .concatWith(editContentStream(request, conversationId))
                .concatWith(Flux.just("\n\n=== 論文生成完成 ===\n\n"))
        )
        .doOnError(error -> log.error("Error during paper generation for conversationId: {}", conversationId, error))
        .doOnTerminate(() -> log.info("Paper generation completed for conversationId: {}", conversationId));
    }

    /**
     * 流式生成大綱（支援文件內容）
     */    private Flux<String> generateOutlineStream(PaperRequest request, List<String> fileContents, String conversationId) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append(String.format("""
            你是一位專業的學術規劃專家 (PlannerAgent)。請為以下論文需求生成詳細且結構化的大綱：
           
            📚 論文基本信息：
            • 主題: %s
            • 類型: %s
            • 學術領域: %s
            • 輸出語言: %s
            • 特殊需求: %s
           """,
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求"
        ));

        // 如果有文件內容，加入到提示中
        if (fileContents != null && !fileContents.isEmpty()) {
            promptBuilder.append("\n\n📄 參考文件內容：\n");
            for (int i = 0; i < fileContents.size(); i++) {
                promptBuilder.append(String.format("=== 文件 %d ===\n%s\n\n", i + 1, fileContents.get(i)));
            }
            promptBuilder.append("**重要**: 請仔細分析上述文件內容，提取關鍵信息、數據、觀點和研究發現，並將其整合到大綱中。");
        }
        
        promptBuilder.append(String.format("""
            
            🎯 請用%s生成包含以下結構的詳細大綱：
            
            1. **論文標題** (具體且吸引人)
            2. **摘要框架** (背景、目的、方法、主要發現、結論，150-300字)
            3. **關鍵詞** (5-8個學術關鍵詞)
            4. **章節結構** (每章節需包含：)
               - 章節標題和編號
               - 主要內容要點 (3-5個具體要點)
               - 預期篇幅指引 (如：引言約800-1200字)
               - 關鍵論證或分析重點
            5. **參考文獻類型** (建議的文獻來源類型)
            
            💡 大綱要求：
            • 邏輯結構清晰，章節間有良好的連接性
            • 每個章節都有具體的內容指導，避免空泛的標題
            • 提供足夠的細節指引，讓後續的Writer能夠充分展開
            • 如果有參考文件，明確指出在哪些章節中如何使用這些信息
            • 確保符合%s論文的學術標準和規範
            
            請開始生成大綱：
            """, 
            request.getLanguage(), 
            request.getType()
        ));
        
        return plannerAgent
            .prompt()
            .user(promptBuilder.toString())
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }

    /**
     * 流式撰寫內容（支援文件內容）
     */    private Flux<String> writeContentStream(PaperRequest request, List<String> fileContents, String conversationId) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append(String.format("""
            你是一位專業的學術寫作專家 (WriterAgent)。請為論文主題"%s"撰寫詳細的學術內容。
            
            📚 論文信息：
            • 論文類型: %s
            • 研究領域: %s
            • 輸出語言: %s
            • 特殊需求: %s
            """,
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求"
        ));

        // 如果有文件內容，提醒使用這些內容
        if (fileContents != null && !fileContents.isEmpty()) {
            promptBuilder.append("\n\n📄 參考文件說明：\n");
            promptBuilder.append("用戶已上傳相關文件內容，PlannerAgent 在制定大綱時已經考慮了這些信息。");
            promptBuilder.append("請在撰寫時充分利用這些文件中的：\n");
            promptBuilder.append("• 具體數據和統計信息\n");
            promptBuilder.append("• 實際案例和研究發現\n"); 
            promptBuilder.append("• 理論框架和概念\n");
            promptBuilder.append("• 相關研究方法和結果\n");
            promptBuilder.append("• 任何可以支持論證的具體信息\n");
        }        promptBuilder.append(String.format("""
            
            🎯 撰寫任務：
            **必須嚴格遵循 PlannerAgent 生成的大綱結構**，根據大綱中的每個章節和要點進行充分展開。
            
            📝 內容要求：
            1. **嚴格按照大綱的章節順序和結構撰寫**
            2. **每個章節都要達到大綱中建議的篇幅**
            3. **充分展開大綱中列出的每個要點**
            4. **保持學術寫作的嚴謹性和專業性**
            5. **如有參考文件，要在適當位置引用和分析**
            6. **確保完整撰寫所有章節，特別是結論部分**
            
            📋 撰寫指南：
            • **引言部分**：研究背景、問題陳述、研究目的、文獻回顧、研究假設
            • **方法部分**：研究設計、數據收集、分析方法、樣本描述
            • **結果部分**：主要發現、數據分析、圖表說明、關鍵洞察
            • **討論部分**：結果解釋、理論意義、實踐價值、局限性分析
            • **結論部分**：主要結論、貢獻總結、未來研究建議、研究局限性
            
            ⚠️ 重要提醒：
            • 不要只寫章節標題，每個部分都要有實質性的詳細內容
            • 確保內容的深度和廣度符合%s的學術標準
            • 使用正確的%s學術寫作風格和術語
            • 保持論證的邏輯性和連貫性
            • **必須完整撰寫所有章節，避免內容截斷**
            • **特別注意結論部分要完整，包含完整的貢獻總結和未來研究方向**
            
            🎯 請開始撰寫完整的論文內容，確保每個章節都充實完整：
            """, 
            request.getType(),
            request.getLanguage()
        ));
        
        return writerAgent
            .prompt()
            .user(promptBuilder.toString())
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }

    /**
     * 流式編輯潤色
     */    private Flux<String> editContentStream(PaperRequest request, String conversationId) {
        String prompt = String.format("""
            你是一位資深的學術編輯專家 (EditorAgent)。請對前面生成的論文進行專業編輯和潤色。
            
            📚 論文信息：
            • 主題: %s
            • 類型: %s
            • 學術領域: %s
            • 輸出語言: %s
            • 特殊需求: %s
            
            🎯 編輯任務：
            請基於 PlannerAgent 的大綱和 WriterAgent 的內容，進行全面的編輯和潤色。
            
            📝 編輯重點：
            
            1. **結構完整性檢查**：
               • 確保所有大綱要求的章節都已包含
               • 檢查章節間的邏輯連接和過渡
               • 驗證內容是否充分回應了研究問題
               • **特別注意：如果WriterAgent的內容不完整，必須補充完成**
            
            2. **內容質量提升**：
               • 增強論證的說服力和邏輯性
               • 補充必要的細節和例證
               • 確保學術深度和廣度符合%s標準
               • 改善關鍵概念的闡述和分析
               • **完善任何被截斷或不完整的部分**
            
            3. **語言和風格優化**：
               • 使用正確的%s學術寫作規範
               • 改善句式結構和表達清晰度
               • 統一術語使用和學術語調
               • 修正語法、拼寫和標點錯誤
            
            4. **學術規範檢查**：
               • 確保引用格式的正確性
               • 檢查圖表和數據的準確表述
               • 驗證學術倫理和客觀性
               • 加強批判性思維的體現
            
            5. **整體連貫性**：
               • 確保摘要準確反映論文內容
               • 檢查結論與引言的呼應
               • 驗證關鍵詞與內容的一致性
               • 提升整體閱讀流暢性
            
            ⚠️ 關鍵要求：
            • **必須提供完整的最終論文版本**，確保所有章節都完整
            • **特別關注結論部分**，必須包含完整的貢獻總結和未來研究建議
            • **如果前面的內容被截斷，請補充完成**
            • **保持原有的結構框架**，在此基礎上進行內容優化
            • **確保編輯後的論文達到發表水準**
            • **維持學術寫作的嚴謹性和專業性**
            
            📋 請確保最終論文包含：
            1. 完整的標題和摘要
            2. 所有章節的詳細內容
            3. 完整的討論部分（結果解釋、理論意義、實踐價值、局限性）
            4. 完整的結論部分（主要結論、貢獻總結、未來研究建議）
            5. 適當的參考文獻格式
            
            請開始編輯並提供最終的完整論文：
            """, 
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求",
            request.getType(),
            request.getLanguage()
        );
        
        return editorAgent
            .prompt()
            .user(prompt)
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }
}

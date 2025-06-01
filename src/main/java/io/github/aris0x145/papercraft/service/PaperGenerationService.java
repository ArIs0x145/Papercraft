package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.PaperRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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
    }

    /**
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
        .doOnTerminate(() -> {
            log.info("Paper generation completed for conversationId: {}", conversationId);
        });
    }

    /**
     * 流式生成大綱（支援文件內容）
     */    private Flux<String> generateOutlineStream(PaperRequest request, List<String> fileContents, String conversationId) {
        StringBuilder promptBuilder = new StringBuilder();        promptBuilder.append(String.format("""
            請為以下論文需求生成詳細大綱：
            
            主題: %s
            類型: %s
            學術領域: %s
            輸出語言: %s
            特殊需求: %s
            """, 
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求"
        ));

        // 如果有文件內容，加入到提示中
        if (fileContents != null && !fileContents.isEmpty()) {
            promptBuilder.append("\n\n=== 參考文件內容 ===\n");
            for (int i = 0; i < fileContents.size(); i++) {
                promptBuilder.append(String.format("--- 文件 %d ---\n%s\n\n", i + 1, fileContents.get(i)));
            }
            promptBuilder.append("請基於上述文件內容，生成更具體和準確的論文大綱。");
        }
        
        promptBuilder.append(String.format("\n\n請用%s生成包含標題、摘要、關鍵詞和詳細章節結構的大綱。", request.getLanguage()));
        
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
        StringBuilder promptBuilder = new StringBuilder();        promptBuilder.append(String.format("""
            請為論文主題"%s"撰寫詳細的學術內容。
            
            論文類型: %s
            研究領域: %s
            輸出語言: %s
            特殊需求: %s
            """, 
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求"
        ));

        // 如果有文件內容，提醒使用這些內容
        if (fileContents != null && !fileContents.isEmpty()) {
            promptBuilder.append("\n\n=== 重要提示 ===\n");
            promptBuilder.append("用戶已上傳了相關文件內容，這些內容在 Planner 階段已經被納入考慮。");
            promptBuilder.append("請確保在撰寫時充分利用這些信息來豐富論文內容，");
            promptBuilder.append("包括引用相關數據、案例、理論或研究發現。");
        }

        promptBuilder.append(String.format("\n\n請根據前面 Planner 生成的大綱，用%s撰寫包含引言、方法、結果、討論和結論的完整內容。", request.getLanguage()));
        
        return writerAgent
            .prompt()
            .user(promptBuilder.toString())
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }

    /**
     * 流式編輯潤色
     */    private Flux<String> editContentStream(PaperRequest request, String conversationId) {        String prompt = String.format("""
            請對前面生成的論文進行專業編輯和潤色：
            
            主題: %s
            類型: %s
            學術領域: %s
            輸出語言: %s
            特殊需求: %s
            
            請根據前面 Planner 的大綱和 Writer 的內容，用%s改善語法、表達和學術寫作風格，
            確保整體連貫性和邏輯性。提供最終的完整論文版本。
            """, 
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getLanguage(),
            request.getRequirements() != null ? request.getRequirements() : "無特殊需求",
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

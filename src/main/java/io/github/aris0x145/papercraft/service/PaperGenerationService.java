package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.PaperRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 論文生成服務 - 基於 Spring AI 標準方式的多 Agent 協同工作
 * 使用 CONVERSATION_ID 參數控制記憶體共享，完全遵循 Spring AI 最佳實踐
 */
@Slf4j
@Service
public class PaperGenerationService {

    private final ChatClient plannerAgent;
    private final ChatClient writerAgent;
    private final ChatClient editorAgent;

    /**
     * 構造函數 - 注入三個 Agent 的 ChatClient
     * 每個 Agent 都已經配置了 MessageChatMemoryAdvisor，支援記憶體共享
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
     * 流式生成論文（即時流式響應）
     * 使用 CONVERSATION_ID 參數實現多 Agent 間的記憶體共享
     */
    public Flux<String> generatePaperStream(PaperRequest request) {
        String conversationId = "paper-" + UUID.randomUUID().toString();
        log.info("Starting paper generation with conversationId: {}", conversationId);
        
        return Flux.concat(
            // 步驟1: 生成大綱
            Flux.just("=== 開始生成論文大綱 ===\n\n")
                .concatWith(generateOutlineStream(request, conversationId))
                .concatWith(Flux.just("\n\n=== 大綱生成完成 ===\n\n")),
            
            // 步驟2: 撰寫內容  
            Flux.just("=== 開始撰寫論文內容 ===\n\n")
                .concatWith(writeContentStream(request, conversationId))
                .concatWith(Flux.just("\n\n=== 內容撰寫完成 ===\n\n")),
            
            // 步驟3: 編輯潤色
            Flux.just("=== 開始編輯潤色 ===\n\n")
                .concatWith(editContentStream(request, conversationId))
                .concatWith(Flux.just("\n\n=== 論文生成完成 ===\n\n"))
        )
        .doOnTerminate(() -> {
            log.info("Paper generation completed for conversationId: {}", conversationId);
        });
    }

    /**
     * 流式生成大綱
     * 使用 CONVERSATION_ID 參數控制記憶體
     */
    private Flux<String> generateOutlineStream(PaperRequest request, String conversationId) {
        String prompt = String.format("""
            請為以下論文需求生成詳細大綱：
            
            主題: %s
            類型: %s
            領域: %s
            目標字數: %d
            特殊要求: %s
            
            請生成包含標題、摘要、關鍵詞和詳細章節結構的大綱。
            """, 
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getWordCount(),
            request.getRequirements() != null ? request.getRequirements() : "無"
        );
        
        return plannerAgent
            .prompt()
            .user(prompt)
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }

    /**
     * 流式撰寫內容
     * 使用相同的 CONVERSATION_ID，Writer 可以看到 Planner 的工作結果
     */
    private Flux<String> writeContentStream(PaperRequest request, String conversationId) {
        String prompt = String.format("""
            請為論文主題"%s"撰寫詳細的學術內容。
            
            論文類型: %s
            研究領域: %s
            目標字數: %d
            特殊要求: %s
            
            請根據前面 Planner 生成的大綱，撰寫包含引言、方法、結果、討論和結論的完整內容。
            """, 
            request.getTopic(),
            request.getType(),
            request.getField(),
            request.getWordCount(),
            request.getRequirements() != null ? request.getRequirements() : "無"
        );
        
        return writerAgent
            .prompt()
            .user(prompt)
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }

    /**
     * 流式編輯潤色
     * 使用相同的 CONVERSATION_ID，Editor 可以看到 Planner 和 Writer 的所有工作結果
     */
    private Flux<String> editContentStream(PaperRequest request, String conversationId) {
        String prompt = String.format("""
            請對前面生成的論文進行專業編輯和潤色：
            
            主題: %s
            類型: %s
            領域: %s
            
            請根據前面 Planner 的大綱和 Writer 的內容，改善語法、表達和學術寫作風格，
            確保整體連貫性和邏輯性。提供最終的完整論文版本。
            """, 
            request.getTopic(),
            request.getType(),
            request.getField()
        );
        
        return editorAgent
            .prompt()
            .user(prompt)
            .advisors(advisorSpec -> advisorSpec.param("CONVERSATION_ID", conversationId))
            .stream()
            .content();
    }
}

package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.PaperRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 論文生成服務 - 基於 Spring AI 的多 Agent 協同工作
 */
@Slf4j
@Service
public class PaperGenerationService {

    private final ChatClient plannerAgent;
    private final ChatClient writerAgent;
    private final ChatClient editorAgent;
    
    /**
     * 手動構造函數 - 解決 Lombok @RequiredArgsConstructor 不複製 @Qualifier 注解的問題
     */
    public PaperGenerationService(
            @Qualifier("plannerAgent") ChatClient plannerAgent,
            @Qualifier("writerAgent") ChatClient writerAgent,
            @Qualifier("editorAgent") ChatClient editorAgent) {        this.plannerAgent = plannerAgent;
        this.writerAgent = writerAgent;
        this.editorAgent = editorAgent;
    }

    /**
     * 流式生成論文（即時流式響應）
     */
    public Flux<String> generatePaperStream(PaperRequest request) {
        String taskId = UUID.randomUUID().toString();
        
        return Flux.concat(
            // 步驟1: 生成大綱
            Flux.just("=== 開始生成論文大綱 ===\n\n")
                .concatWith(generateOutlineStream(request, taskId))
                .concatWith(Flux.just("\n\n=== 大綱生成完成 ===\n\n")),
            
            // 步驟2: 撰寫內容  
            Flux.just("=== 開始撰寫論文內容 ===\n\n")
                .concatWith(writeContentStream(request, taskId))
                .concatWith(Flux.just("\n\n=== 內容撰寫完成 ===\n\n")),
            
            // 步驟3: 編輯潤色
            Flux.just("=== 開始編輯潤色 ===\n\n")
                .concatWith(editContentStream(request, taskId))
                .concatWith(Flux.just("\n\n=== 論文生成完成 ===\n\n"))
        );
    }/**
     * 流式生成大綱
     */
    private Flux<String> generateOutlineStream(PaperRequest request, String taskId) {
        String taskConversationId = "task-" + taskId;
        String plannerConversationId = taskConversationId + "-planner";
        
        // 先在任務級別記錄基礎需求，讓所有 Agent 都能看到
        plannerAgent
            .prompt()
            .user("記錄任務需求：主題=" + request.getTopic() + ", 類型=" + request.getType() + ", 領域=" + request.getField() + ", 字數=" + request.getWordCount())
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, taskConversationId))
            .stream()
            .content()
            .collectList()
            .map(chunks -> String.join("", chunks))
            .block(); // 同步執行以確保基礎上下文先建立
        
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
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, plannerConversationId))
            .stream()
            .content();
    }    /**
     * 流式撰寫內容
     */
    private Flux<String> writeContentStream(PaperRequest request, String taskId) {
        String taskConversationId = "task-" + taskId;
        String writerConversationId = taskConversationId + "-writer";
        
        // 先讓 Writer 了解任務背景（讀取任務級別記憶）
        writerAgent
            .prompt()
            .user("我需要了解這個論文任務的背景和 Planner 的規劃，請簡單總結一下。")
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, taskConversationId))
            .stream()
            .content()
            .collectList()
            .map(chunks -> String.join("", chunks))
            .block(); // 同步執行以確保能看到前面的上下文
        
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
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, writerConversationId))
            .stream()
            .content();
    }    /**
     * 流式編輯潤色
     */
    private Flux<String> editContentStream(PaperRequest request, String taskId) {
        String taskConversationId = "task-" + taskId;
        String editorConversationId = taskConversationId + "-editor";
        
        // 先了解任務背景和前面 Agent 的工作
        editorAgent
            .prompt()
            .user("我需要了解這篇論文的寫作背景和目標，以及前面 Planner 和 Writer 的工作成果，請簡單總結一下。")
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, taskConversationId))
            .stream()
            .content()
            .collectList()
            .map(chunks -> String.join("", chunks))
            .block(); // 同步執行以確保能看到前面的所有上下文
        
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
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, editorConversationId))
            .stream()
            .content();
    }
}

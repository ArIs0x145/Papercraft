package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.PaperOutline;
import io.github.aris0x145.papercraft.model.PaperRequest;
import io.github.aris0x145.papercraft.model.PaperResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
            @Qualifier("editorAgent") ChatClient editorAgent) {
        this.plannerAgent = plannerAgent;
        this.writerAgent = writerAgent;
        this.editorAgent = editorAgent;
    }

    private final Map<String, PaperResult> results = new ConcurrentHashMap<>();

    /**
     * 開始生成論文（異步處理）
     */
    public Mono<String> startGeneration(PaperRequest request) {
        String taskId = UUID.randomUUID().toString();
        
        // 創建初始結果
        PaperResult result = new PaperResult();
        result.setId(taskId);
        result.setStatus(PaperResult.GenerationStatus.PLANNING);
        result.setProgress(0);
        result.setCreatedAt(LocalDateTime.now());
        results.put(taskId, result);
        
        // 異步處理論文生成
        generatePaperAsync(taskId, request)
            .subscribe(
                generatedResult -> {
                    log.info("論文生成完成: {}", taskId);
                    results.put(taskId, generatedResult);
                },
                error -> {
                    log.error("論文生成失敗: {}", taskId, error);
                    result.setStatus(PaperResult.GenerationStatus.FAILED);
                    result.setErrorMessage(error.getMessage());
                    results.put(taskId, result);
                }
            );
            
        return Mono.just(taskId);
    }

    /**
     * 獲取生成進度
     */
    public Mono<PaperResult> getProgress(String taskId) {
        PaperResult result = results.get(taskId);
        if (result == null) {
            return Mono.error(new RuntimeException("Task not found: " + taskId));
        }
        return Mono.just(result);
    }

    /**
     * 獲取完成的論文
     */
    public Mono<PaperResult> getResult(String taskId) {
        return getProgress(taskId)
            .filter(result -> result.getStatus() == PaperResult.GenerationStatus.COMPLETED)
            .switchIfEmpty(Mono.error(new RuntimeException("Paper not completed yet")));
    }

    /**
     * 異步生成論文的核心邏輯
     */
    private Mono<PaperResult> generatePaperAsync(String taskId, PaperRequest request) {
        return Mono.fromCallable(() -> results.get(taskId))
            .flatMap(result -> {
                // 步驟1: 生成大綱 (PlannerAgent)
                return generateOutline(request)
                    .doOnNext(outline -> {
                        result.setOutline(outline);
                        result.setTitle(outline.getTitle());
                        result.setProgress(25);
                        results.put(taskId, result);
                    })
                    // 步驟2: 撰寫內容 (WriterAgent)
                    .flatMap(this::writeContent)
                    .doOnNext(content -> {
                        result.setStatus(PaperResult.GenerationStatus.WRITING);
                        result.setSectionContents(content);
                        result.setProgress(75);
                        results.put(taskId, result);
                    })
                    // 步驟3: 編輯潤色 (EditorAgent)
                    .flatMap(this::editContent)
                    .doOnNext(finalContent -> {
                        result.setStatus(PaperResult.GenerationStatus.EDITING);
                        result.setProgress(90);
                        results.put(taskId, result);
                    })
                    // 完成
                    .map(finalContent -> {
                        result.setContent(finalContent);
                        result.setStatus(PaperResult.GenerationStatus.COMPLETED);
                        result.setProgress(100);
                        result.setCompletedAt(LocalDateTime.now());
                        return result;
                    });
            });
    }    /**
     * 使用 PlannerAgent 生成論文大綱
     */
    private Mono<PaperOutline> generateOutline(PaperRequest request) {
        return Mono.fromCallable(() -> {
            log.info("開始生成論文大綱: {}", request.getTopic());
            
            // 為此任務創建唯一的對話 ID
            String conversationId = "planner-" + UUID.randomUUID();
            
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
            
            // 使用官方推薦的 .entity() 方法進行結構化輸出
            return plannerAgent
                .prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(PaperOutline.class);
        });
    }/**
     * 使用 WriterAgent 撰寫論文內容
     */
    private Mono<Map<String, String>> writeContent(PaperOutline outline) {
        return Mono.fromCallable(() -> {
            log.info("開始撰寫論文內容: {}", outline.getTitle());
            
            // 為此任務創建唯一的對話 ID
            String conversationId = "writer-" + UUID.randomUUID();
            
            Map<String, String> sectionContents = new HashMap<>();
            
            // 為每個章節生成內容
            for (PaperOutline.Section section : outline.getSections()) {
                String sectionPrompt = String.format("""
                    請為以下章節撰寫詳細內容：
                    
                    論文標題: %s
                    章節標題: %s
                    章節描述: %s
                    預估字數: %d
                    子章節: %s
                    
                    請撰寫學術性強、邏輯清晰的內容。
                    """,
                    outline.getTitle(),
                    section.getTitle(),
                    section.getDescription(),
                    section.getEstimatedWords(),
                    section.getSubsections() != null ? String.join(", ", section.getSubsections()) : "無"
                );
                
                String content = writerAgent
                    .prompt()
                    .user(sectionPrompt)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
                    
                sectionContents.put(section.getTitle(), content);
            }
            
            return sectionContents;
        });
    }    /**
     * 使用 EditorAgent 編輯和潤色論文
     */
    private Mono<String> editContent(Map<String, String> sectionContents) {
        return Mono.fromCallable(() -> {
            log.info("開始編輯潤色論文");
            
            // 為此任務創建唯一的對話 ID
            String conversationId = "editor-" + UUID.randomUUID();
            
            // 將所有章節內容合併
            StringBuilder fullPaper = new StringBuilder();
            for (Map.Entry<String, String> entry : sectionContents.entrySet()) {
                fullPaper.append("## ").append(entry.getKey()).append("\n\n");
                fullPaper.append(entry.getValue()).append("\n\n");
            }
            
            String editPrompt = String.format("""
                請對以下論文進行專業的編輯和潤色：
                
                %s
                
                請改善語法、表達和學術寫作風格，確保整體連貫性。
                """, fullPaper);
            
            return editorAgent
                .prompt()
                .user(editPrompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        });
    }
}

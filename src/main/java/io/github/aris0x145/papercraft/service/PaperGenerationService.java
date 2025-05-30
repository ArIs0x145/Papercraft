package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.PaperOutline;
import io.github.aris0x145.papercraft.model.PaperRequest;
import io.github.aris0x145.papercraft.model.PaperResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

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
            .flatMap(result -> {            // 步驟1: 生成大綱 (PlannerAgent)
                return generateOutline(request, taskId)
                    .doOnNext(outline -> {
                        result.setOutline(outline);
                        result.setTitle(outline.getTitle());
                        result.setProgress(25);
                        results.put(taskId, result);
                    })
                    // 步驟2: 撰寫內容 (WriterAgent)
                    .flatMap(outline -> writeContent(outline, taskId))
                    .doOnNext(content -> {
                        result.setStatus(PaperResult.GenerationStatus.WRITING);
                        result.setSectionContents(content);
                        result.setProgress(75);
                        results.put(taskId, result);
                    })
                    // 步驟3: 編輯潤色 (EditorAgent)
                    .flatMap(content -> editContent(content, taskId))
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
    private Mono<PaperOutline> generateOutline(PaperRequest request, String taskId) {
        return Mono.fromCallable(() -> {
            log.info("開始生成論文大綱: {}", request.getTopic());
            
            // 優化：使用任務級別的 conversationId，讓所有 Agent 可以共享基礎上下文
            String taskConversationId = "task-" + taskId;
            String plannerConversationId = taskConversationId + "-planner";
            
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
              // 先在任務級別記錄基礎需求，讓所有 Agent 都能看到
            plannerAgent
                .prompt()
                .user("記錄任務需求：主題=" + request.getTopic() + ", 類型=" + request.getType() + ", 領域=" + request.getField() + ", 字數=" + request.getWordCount())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, taskConversationId))
                .stream()
                .content()
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block(); // 這裡保持同步以簡化邏輯
              // 然後在 Planner 專用記憶中進行詳細規劃
            BeanOutputConverter<PaperOutline> converter = new BeanOutputConverter<>(PaperOutline.class);
            String streamResult = plannerAgent
                .prompt()
                .user(prompt + "\n\n" + converter.getFormat())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, plannerConversationId))
                .stream()
                .content()
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
            
            return converter.convert(streamResult);
        });
    }/**
     * 使用 WriterAgent 撰寫論文內容
     */
    private Mono<Map<String, String>> writeContent(PaperOutline outline, String taskId) {
        return Mono.fromCallable(() -> {
            log.info("開始撰寫論文內容: {}", outline.getTitle());
            
            // 優化：Writer 可以看到任務基礎資訊 + 自己的專用記憶
            String taskConversationId = "task-" + taskId;
            String writerConversationId = taskConversationId + "-writer";
            
            Map<String, String> sectionContents = new HashMap<>();
              // 先讓 Writer 了解任務背景（讀取任務級別記憶）
            writerAgent
                .prompt()
                .user("我需要了解這個論文任務的背景和 Planner 的規劃，請簡單總結一下。")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, taskConversationId))
                .stream()
                .content()
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
            
            // 然後在專用記憶中進行寫作
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
                );                String content = writerAgent
                    .prompt()
                    .user(sectionPrompt)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, writerConversationId))
                    .stream()
                    .content()
                    .collectList()
                    .map(chunks -> String.join("", chunks))
                    .block();
                    
                sectionContents.put(section.getTitle(), content);
            }
            
            return sectionContents;
        });
    }    /**
     * 使用 EditorAgent 編輯和潤色論文
     */
    private Mono<String> editContent(Map<String, String> sectionContents, String taskId) {
        return Mono.fromCallable(() -> {
            log.info("開始編輯潤色論文");
            
            // 優化：Editor 也可以看到完整的任務上下文
            String taskConversationId = "task-" + taskId;
            String editorConversationId = taskConversationId + "-editor";
              // 先了解任務背景和前面 Agent 的工作
            editorAgent
                .prompt()
                .user("我需要了解這篇論文的寫作背景和目標，請簡單總結一下。")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, taskConversationId))
                .stream()
                .content()
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
            
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
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, editorConversationId))
                .stream()
                .content()
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
        });
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
    }    /**
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

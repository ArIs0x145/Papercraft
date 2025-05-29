package io.github.aris0x145.papercraft.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * ChatClient 配置
 */
@Configuration
public class ChatClientConfig {
    /**
     * 聊天記憶體儲存庫 - 使用記憶體儲存
     * <p>
     * 注意：根據官方文檔，Spring AI 會自動配置一個 ChatMemory bean
     * 但我們這裡手動配置以支持多個獨立的記憶體實例
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }    /**
     * 共享的 ChatMemory 實例
     * 根據官方文檔，Spring AI 默認自動配置一個 ChatMemory bean
     * 我們這裡創建一個用於共享的實例
     */
    @Bean
    public ChatMemory sharedChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(20)
            .build();
    }    /**
     * 論文生成專用的日誌 Advisor
     * 根據 Spring AI 官方文檔實現 SimpleLoggerAdvisor
     * 自動記錄 AdvisedRequest 和 AdvisedResponse，包含 token 使用量
     */
    @Bean
    public SimpleLoggerAdvisor paperGenerationLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
    
    /**
     * PlannerAgent 專用ChatClient - 負責分析需求並生成論文大綱
     */
    @Bean
    public ChatClient plannerAgent(ChatClient.Builder builder, ChatMemoryRepository chatMemoryRepository, SimpleLoggerAdvisor loggerAdvisor) {
        ChatMemory memory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(20)
            .build();
            
        return builder
            .defaultSystem("""
                你是一位專業的學術論文規劃專家。你的任務是：
                1. 分析用戶的論文需求
                2. 生成詳細的論文大綱
                3. 確保大綱結構合理且符合學術標準
                
                請以 JSON 格式回應，包含：title（標題）、abstractText（摘要）、keywords（關鍵詞列表）、sections（章節列表，每個章節包含 title、description、estimatedWords、subsections）
                """)
            .defaultAdvisors(List.of(
                MessageChatMemoryAdvisor.builder(memory).build(),
                loggerAdvisor
            ))
            .build();
    }    /**
     * WriterAgent 專用ChatClient - 負責根據大綱撰寫論文內容
     */
    @Bean
    public ChatClient writerAgent(ChatClient.Builder builder, ChatMemoryRepository chatMemoryRepository, SimpleLoggerAdvisor loggerAdvisor) {
        ChatMemory memory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(30)
            .build();
            
        return builder
            .defaultSystem("""
                你是一位專業的學術寫作助手。你的任務是：
                1. 根據提供的大綱撰寫高質量的論文內容
                2. 確保內容學術性強、邏輯清晰
                3. 保持統一的寫作風格和術語使用
                
                請撰寫清晰、專業的學術內容，適當使用專業術語。
                """)
            .defaultAdvisors(List.of(
                MessageChatMemoryAdvisor.builder(memory).build(),
                loggerAdvisor
            ))
            .build();
    }    /**
     * EditorAgent 專用ChatClient - 負責編輯和潤色論文
     */
    @Bean
    public ChatClient editorAgent(ChatClient.Builder builder, ChatMemoryRepository chatMemoryRepository, SimpleLoggerAdvisor loggerAdvisor) {
        ChatMemory memory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(15)
            .build();
            
        return builder
            .defaultSystem("""
                你是一位專業的學術編輯。你的任務是：
                1. 檢查並改善論文的語法和表達
                2. 確保論文結構邏輯清晰
                3. 優化學術寫作風格
                
                請對論文進行專業的編輯和潤色，提高整體質量。
                """)
            .defaultAdvisors(List.of(
                MessageChatMemoryAdvisor.builder(memory).build(),
                loggerAdvisor
            ))
            .build();
    }
}

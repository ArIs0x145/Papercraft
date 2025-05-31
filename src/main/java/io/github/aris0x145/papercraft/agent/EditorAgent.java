package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EditorAgent 配置 - 按照 Spring AI 標準方式實作
 * 使用自動配置的 ChatMemory + MessageChatMemoryAdvisor + CONVERSATION_ID 參數控制
 */
@Configuration
public class EditorAgent {

    /**
     * EditorAgent 專用 ChatClient - 負責編輯和潤色論文
     * 按照 Spring AI 標準方式實作：使用自動配置的 ChatMemory 和 Advisor
     */
    @Bean
    public ChatClient editorAgentChatClient(
            ChatClient.Builder builder, 
            ChatMemory chatMemory, 
            SimpleLoggerAdvisor loggerAdvisor) {
        
        return builder
            .defaultSystem("""
                你是一位專業的學術編輯。你的任務是：
                1. 檢查並改善論文的語法和表達
                2. 確保論文結構邏輯清晰
                3. 優化學術寫作風格
                
                請對論文進行專業的編輯和潤色，提高整體質量。
                """)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                loggerAdvisor
            )
            .build();
    }
}
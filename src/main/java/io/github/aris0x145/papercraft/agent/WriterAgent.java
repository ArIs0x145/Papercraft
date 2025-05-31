package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WriterAgent 配置 - 按照 Spring AI 標準方式實作
 * 使用自動配置的 ChatMemory + MessageChatMemoryAdvisor + CONVERSATION_ID 參數控制
 */
@Configuration
public class WriterAgent {

    /**
     * WriterAgent 專用 ChatClient - 負責根據大綱撰寫論文內容
     * 按照 Spring AI 標準方式實作：使用自動配置的 ChatMemory 和 Advisor
     */
    @Bean
    public ChatClient writerAgentChatClient(
            ChatClient.Builder builder, 
            ChatMemory chatMemory, 
            SimpleLoggerAdvisor loggerAdvisor) {
        
        return builder
            .defaultSystem("""
                你是一位專業的學術寫作助手。你的任務是：
                1. 根據提供的大綱撰寫高質量的論文內容
                2. 確保內容學術性強、邏輯清晰
                3. 保持統一的寫作風格和術語使用
                
                請撰寫清晰、專業的學術內容，適當使用專業術語。
                """)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                loggerAdvisor
            )
            .build();
    }
}
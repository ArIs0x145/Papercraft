package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PlannerAgent 配置 - 按照 Spring AI 標準方式實作
 * 使用自動配置的 ChatMemory + MessageChatMemoryAdvisor + CONVERSATION_ID 參數控制
 */
@Configuration
public class PlannerAgent {

    /**
     * PlannerAgent 專用 ChatClient - 負責分析需求並生成論文大綱
     * 按照 Spring AI 標準方式實作：使用自動配置的 ChatMemory 和 Advisor
     */
    @Bean
    public ChatClient plannerAgentChatClient(
            ChatClient.Builder builder, 
            ChatMemory chatMemory, 
            SimpleLoggerAdvisor loggerAdvisor) {
        
        return builder
            .defaultSystem("""
                你是一位專業的學術論文規劃專家。你的任務是：
                1. 分析用戶的論文需求
                2. 生成詳細的論文大綱
                3. 確保大綱結構合理且符合學術標準
                
                請以 JSON 格式回應，包含：title（標題）、abstractText（摘要）、keywords（關鍵詞列表）、sections（章節列表，每個章節包含 title、description、estimatedWords、subsections）
                """)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                loggerAdvisor
            )
            .build();
    }
}
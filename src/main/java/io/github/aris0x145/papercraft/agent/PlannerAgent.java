package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PlannerAgent 配置
 */
@Configuration
public class PlannerAgent {

    /**
     * PlannerAgent 專用 ChatClient - 負責分析需求並生成論文大綱
     */
    @Bean
    public ChatClient plannerAgentChatClient(
            ChatClient.Builder builder, 
            ChatMemory chatMemory, 
            SimpleLoggerAdvisor loggerAdvisor) {
        
        return builder
            .defaultSystem("""
                你是一位專業的學術寫作專家，擅長多種學科的論文撰寫。你的職責是：
                
                【寫作原則】
                1. 保持客觀、嚴謹的學術語調
                2. 使用準確的專業術語和概念
                3. 確保論證邏輯清晰、證據充分
                4. 遵循學科特有的寫作慣例
                
                【內容標準】
                - 每個段落都有明確的主題句和支撐論據
                - 適當引用權威文獻支持論點
                - 使用過渡句確保段落間連貫性
                - 避免主觀臆測，基於客觀分析
                
                【格式要求】
                - 遵循標準學術引用格式 (APA/IEEE/等)
                - 使用正式的學術寫作風格
                - 適當使用圖表和數據支撐論點
                - 保持用詞準確性和表達簡潔性
                
                【質量控制】
                - 確保每個論點都有充分的理論或實證支持
                - 避免重複和冗餘表達
                - 保持全文風格和術語的一致性
                """)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                loggerAdvisor
            )
            .build();
    }
}
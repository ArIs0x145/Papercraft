package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EditorAgent 配置
 */
@Configuration
public class EditorAgent {

    /**
     * EditorAgent 專用 ChatClient - 負責編輯和潤色論文
     */
    @Bean
    public ChatClient editorAgentChatClient(
            ChatClient.Builder builder, 
            ChatMemory chatMemory, 
            SimpleLoggerAdvisor loggerAdvisor) {
        
        return builder
            .defaultSystem("""
                你是一位資深的學術論文規劃專家，具有多領域研究經驗。你的職責是：
                
                【核心任務】
                1. 深度分析研究主題的學術背景和現狀
                2. 設計符合學術標準的論文架構
                3. 確保邏輯層次清晰、論證結構合理
                4. 提供具體的章節內容指導
                
                【專業標準】
                - 遵循國際學術論文撰寫規範
                - 確保研究問題具有學術價值和創新性
                - 設計合理的研究方法論框架
                - 考慮文獻綜述的完整性和深度
                
                【輸出要求】
                - 提供結構化的大綱，包含每個章節的核心論點
                - 估算各章節的合理字數分配
                - 標註關鍵研究方法和理論框架
                - 建議相關文獻檢索關鍵詞
                """)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                loggerAdvisor
            )
            .build();
    }
}
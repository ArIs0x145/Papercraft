package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WriterAgent {

    /**
     * WriterAgent 專用 ChatClient - 負責根據大綱撰寫論文內容
     */
    @Bean
    public ChatClient writerAgentChatClient(
            ChatClient.Builder builder, 
            ChatMemory chatMemory, 
            SimpleLoggerAdvisor loggerAdvisor) {
        return builder
            .defaultSystem("""
                你是一位專業的學術寫作專家，擅長多種學科的論文撰寫。你的職責是：
                
                【核心任務】
                1. **嚴格遵循 PlannerAgent 制定的大綱結構**
                2. **充分展開每個章節的內容要點**
                3. **確保內容深度和廣度符合學術標準**
                4. **整合參考文件中的相關信息**
                
                【寫作原則】
                - 保持客觀、嚴謹的學術語調
                - 使用準確的專業術語和概念
                - 確保論證邏輯清晰、證據充分
                - 遵循學科特有的寫作慣例
                - **每個章節都要有實質性的詳細內容，不能只是空泛的標題**
                
                【內容標準】
                - 每個段落都有明確的主題句和支撐論據
                - 適當引用權威文獻支持論點
                - 使用過渡句確保段落間連貫性
                - 避免主觀臆測，基於客觀分析
                - **確保達到大綱中建議的篇幅要求**
                
                【結構要求】
                - **引言**：包含研究背景、問題陳述、研究目的、文獻回顧、研究假設
                - **方法**：研究設計、數據收集、分析方法、樣本描述等詳細內容
                - **結果**：主要發現、數據分析、圖表說明、關鍵洞察
                - **討論**：結果解釋、理論意義、實踐價值、局限性分析
                - **結論**：主要結論、貢獻總結、未來研究建議
                  【品質要求】
                - 確保每個論點都有充分的理論或實證支持
                - 避免重複和冗餘表達
                - 保持全文風格和術語的一致性
                - **提供完整、詳細、有深度的學術內容**
                """)
            .defaultOptions(OpenAiChatOptions.builder()
                .withModel("gpt-4o-mini")
                .withTemperature(0.7f)
                .withMaxTokens(16384)
                .build()
            )
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                loggerAdvisor
            )
            .build();
    }
}
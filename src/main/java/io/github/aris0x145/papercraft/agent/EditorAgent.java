package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                你是一位資深的學術編輯，具有豐富的論文審校經驗。你的職責是：
                
                【核心任務】
                1. **驗證 WriterAgent 是否完全遵循了 PlannerAgent 的大綱**
                2. **確保所有章節內容充實且符合學術標準**
                3. **提供完整的最終論文版本**
                4. **提升整體品質到發表水準**
                
                【編輯重點】
                1. 結構完整性和內容充實度檢查
                2. 語法和語言表達的準確性
                3. 學術寫作風格的一致性
                4. 論證邏輯的清晰性和完整性
                5. 格式規範的標準化
                
                【檢查清單】
                結構層面：
                - 是否包含大綱要求的所有章節和要點
                - 各章節內容是否充實且有深度
                - 標題是否準確反映研究內容
                - 摘要是否包含研究目的、方法、結果、結論
                - 各章節是否邏輯連貫、過渡自然
                - 結論是否呼應研究問題
                
                內容層面：
                - 每個章節是否達到足夠的篇幅和深度
                - 論點是否有充分的文獻支撐
                - 數據和圖表是否準確清晰
                - 專業術語使用是否準確一致
                - 是否存在邏輯漏洞或矛盾
                - 參考文件內容是否得到充分利用
                
                語言層面：
                - 語法錯誤和拼寫錯誤
                - 句式結構是否符合學術寫作標準
                - 用詞是否準確、正式
                - 引用格式是否規範統一
                
                【品質提升】
                - 建議更精確的表達方式
                - 優化句式結構提高可讀性                - 強化論證的說服力
                - 確保整體風格專業統一
                - **補充不足的內容，確保論文完整性**
                """)
            .defaultOptions(OpenAiChatOptions.builder()
                .withModel("gpt-4o-mini")
                .withTemperature(0.3f)
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
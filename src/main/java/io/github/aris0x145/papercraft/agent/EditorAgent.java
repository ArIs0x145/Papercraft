package io.github.aris0x145.papercraft.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
                
                【編輯重點】
                1. 語法和語言表達的準確性
                2. 學術寫作風格的一致性
                3. 論證邏輯的清晰性和完整性
                4. 格式規範的標準化
                
                【檢查清單】
                結構層面：
                - 標題是否準確反映研究內容
                - 摘要是否包含研究目的、方法、結果、結論
                - 各章節是否邏輯連貫、過渡自然
                - 結論是否呼應研究問題
                
                內容層面：
                - 論點是否有充分的文獻支撐
                - 數據和圖表是否準確清晰
                - 專業術語使用是否準確一致
                - 是否存在邏輯漏洞或矛盾
                
                語言層面：
                - 語法錯誤和拼寫錯誤
                - 句式結構是否符合學術寫作標準
                - 用詞是否準確、正式
                - 引用格式是否規範統一
                
                【品質提升】
                - 建議更精確的表達方式
                - 優化句式結構提高可讀性
                - 強化論證的說服力
                - 確保整體風格專業統一
                """)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                loggerAdvisor
            )
            .build();
    }
}
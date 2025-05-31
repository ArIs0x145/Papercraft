package io.github.aris0x145.papercraft.config;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置 - 按照 Spring AI 標準方式實作
 * 使用 Spring AI 自動配置的 ChatMemory，不需要手動配置記憶體管理
 */
@Configuration
public class ChatClientConfig {

    /**
     * 論文生成專用的日誌 Advisor
     * 根據 Spring AI 官方文檔實作 SimpleLoggerAdvisor
     * 自動記錄 AdvisedRequest 和 AdvisedResponse，包含 token 使用量
     */
    @Bean
    public SimpleLoggerAdvisor paperGenerationLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
}

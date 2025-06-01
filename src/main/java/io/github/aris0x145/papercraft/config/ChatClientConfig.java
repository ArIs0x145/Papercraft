package io.github.aris0x145.papercraft.config;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置
 */
@Configuration
public class ChatClientConfig {

    /**
     * 專用的日誌 Advisor
     */
    @Bean
    public SimpleLoggerAdvisor paperGenerationLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
}

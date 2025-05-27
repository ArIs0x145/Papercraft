package io.github.aris0x145.papercraft.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置類
 * 配置 CORS、文件上傳等 Web 相關設定
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    /**
     * 配置 CORS 設定
     * 允許前端應用跨域請求
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

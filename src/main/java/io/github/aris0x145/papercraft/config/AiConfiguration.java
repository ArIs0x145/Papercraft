package io.github.aris0x145.papercraft.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置類
 * 負責配置 ChatClient、VectorStore 和相關的 AI 組件
 */
@Configuration
public class AiConfiguration {

    /**
     * 配置向量儲存庫
     * 使用 SimpleVectorStore 作為記憶體向量儲存
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

    /**
     * 配置主要的 ChatClient
     * 包含 RAG 功能和預設的系統提示詞
     */
    @Bean("mainChatClient")
    public ChatClient mainChatClient(ChatModel chatModel, VectorStore vectorStore) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    你是一個專業的學術報告撰寫助手，具備以下特點：
                    
                    1. 專業性：精通各種學術寫作規範和格式
                    2. 客觀性：基於提供的文獻和資料進行分析，避免主觀臆測
                    3. 結構性：能夠組織清晰、邏輯嚴謹的報告結構
                    4. 中文表達：使用繁體中文進行專業學術寫作
                    5. 引用規範：正確引用和標註資料來源
                    
                    請根據用戶提供的文獻資料和要求，生成高質量的學術報告。
                    如果提供的資料不足以回答某個問題，請明確說明並建議需要補充的資料類型。
                    """)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(org.springframework.ai.vectorstore.SearchRequest.builder()
                                        .similarityThreshold(0.75)
                                        .topK(5)
                                        .build())
                                .build()
                )
                .build();
    }

    /**
     * 配置文檔處理專用的 ChatClient
     * 用於處理和分析上傳的文檔
     */
    @Bean("documentAnalysisChatClient")
    public ChatClient documentAnalysisChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    你是一個專業的文檔分析助手，負責：
                    
                    1. 分析文檔內容的主要主題和關鍵概念
                    2. 提取重要的研究發現和數據
                    3. 識別文檔的類型（研究論文、報告、書籍章節等）
                    4. 生成文檔摘要和關鍵詞
                    
                    請以結構化的方式分析文檔，為後續的報告生成提供基礎。
                    """)
                .build();
    }
}

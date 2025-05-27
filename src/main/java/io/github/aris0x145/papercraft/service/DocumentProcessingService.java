package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.dto.response.DocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文檔處理服務
 * 負責文檔的上傳、解析、向量化和儲存
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final VectorStore vectorStore;
    
    @Qualifier("documentAnalysisChatClient")
    private final ChatClient documentAnalysisChatClient;

    /**
     * 處理上傳的文檔
     */
    public DocumentUploadResponse processDocument(MultipartFile file) {
        String documentId = UUID.randomUUID().toString();
        log.info("開始處理文檔: {} (ID: {})", file.getOriginalFilename(), documentId);

        try {
            // 驗證檔案
            validateFile(file);

            // 創建初始響應
            DocumentUploadResponse.DocumentUploadResponseBuilder responseBuilder = DocumentUploadResponse.builder()
                    .documentId(documentId)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .uploadTime(LocalDateTime.now())
                    .status(DocumentUploadResponse.ProcessingStatus.PROCESSING);

            // 解析文檔
            List<Document> documents = parseDocument(file);
            log.info("文檔解析完成，共 {} 個文檔片段", documents.size());

            // 分割文本
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocuments = splitter.apply(documents);
            log.info("文本分割完成，共 {} 個片段", splitDocuments.size());

            // 為每個文檔片段添加元數據
            splitDocuments.forEach(doc -> {
                doc.getMetadata().put("documentId", documentId);
                doc.getMetadata().put("originalFileName", file.getOriginalFilename());
                doc.getMetadata().put("uploadTime", LocalDateTime.now().toString());
                doc.getMetadata().put("fileType", file.getContentType());
            });

            // 儲存到向量資料庫
            vectorStore.add(splitDocuments);
            log.info("文檔已成功儲存到向量資料庫");

            // 分析文檔內容
            String documentContent = documents.stream()
                    .map(Document::getContent)
                    .reduce("", (a, b) -> a + "\n" + b);

            DocumentAnalysisResult analysisResult = analyzeDocument(documentContent);

            // 完成響應構建
            return responseBuilder
                    .status(DocumentUploadResponse.ProcessingStatus.COMPLETED)
                    .processedUnits(splitDocuments.size())
                    .summary(analysisResult.getSummary())
                    .keywords(analysisResult.getKeywords())
                    .build();

        } catch (Exception e) {
            log.error("處理文檔時發生錯誤: {}", e.getMessage(), e);
            return DocumentUploadResponse.builder()
                    .documentId(documentId)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .uploadTime(LocalDateTime.now())
                    .status(DocumentUploadResponse.ProcessingStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 驗證上傳的檔案
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("檔案不能為空");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("無法確定檔案類型");
        }

        // 支援的檔案類型
        List<String> supportedTypes = List.of(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "text/plain"
        );

        if (!supportedTypes.contains(contentType)) {
            throw new IllegalArgumentException("不支援的檔案類型: " + contentType);
        }

        // 檔案大小限制（50MB）
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("檔案大小超過限制 (50MB)");
        }
    }

    /**
     * 解析文檔內容
     */
    private List<Document> parseDocument(MultipartFile file) throws Exception {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        String contentType = file.getContentType();

        if ("application/pdf".equals(contentType)) {
            // 使用 PDF 讀取器
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            return pdfReader.get();
        } else {
            // 使用 Tika 讀取器處理其他格式
            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            return tikaReader.get();
        }
    }

    /**
     * 分析文檔內容
     */
    private DocumentAnalysisResult analyzeDocument(String content) {
        try {
            String analysisPrompt = String.format("""
                請分析以下文檔內容，並提供：
                1. 一個簡潔的摘要（200字以內）
                2. 5-10個關鍵詞（用逗號分隔）
                
                文檔內容：
                %s
                
                請以以下格式回覆：
                摘要：[這裡是摘要]
                關鍵詞：[關鍵詞1, 關鍵詞2, 關鍵詞3...]
                """, content.length() > 5000 ? content.substring(0, 5000) + "..." : content);

            String analysisResult = documentAnalysisChatClient.prompt()
                    .user(analysisPrompt)
                    .call()
                    .content();

            return parseAnalysisResult(analysisResult);

        } catch (Exception e) {
            log.warn("文檔分析失敗，使用預設值: {}", e.getMessage());
            return new DocumentAnalysisResult("文檔內容分析", List.of("文檔", "內容"));
        }
    }

    /**
     * 解析分析結果
     */
    private DocumentAnalysisResult parseAnalysisResult(String result) {
        try {
            String[] lines = result.split("\n");
            String summary = "";
            List<String> keywords = List.of();

            for (String line : lines) {
                if (line.startsWith("摘要：")) {
                    summary = line.substring(3).trim();
                } else if (line.startsWith("關鍵詞：")) {
                    String keywordStr = line.substring(4).trim();
                    keywords = List.of(keywordStr.split("\\s*,\\s*"));
                }
            }

            return new DocumentAnalysisResult(summary, keywords);
        } catch (Exception e) {
            log.warn("解析分析結果失敗: {}", e.getMessage());
            return new DocumentAnalysisResult("文檔內容", List.of("內容"));
        }
    }

    /**
     * 文檔分析結果內部類
     */
    private record DocumentAnalysisResult(String summary, List<String> keywords) {
        public String getSummary() { return summary; }
        public List<String> getKeywords() { return keywords; }
    }
}

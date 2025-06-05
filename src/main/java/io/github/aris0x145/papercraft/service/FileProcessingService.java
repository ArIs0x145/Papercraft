package io.github.aris0x145.papercraft.service;

import io.github.aris0x145.papercraft.model.FileProcessingResult;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.*;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件處理服務
 */
@Slf4j
@Service
public class FileProcessingService {

    private final TokenTextSplitter textSplitter;

    public FileProcessingService() {
        this.textSplitter = new TokenTextSplitter(
            2000,
            400,
            10,
            50,
            true
        );
    }

    /**
     * 處理上傳的文件列表
     */
    public FileProcessingResult processFiles(List<MultipartFile> files) {
        log.info("開始處理 {} 個上傳文件", files.size());
        
        FileProcessingResult result = new FileProcessingResult();
        result.setProcessedFiles(new ArrayList<>());
        result.setFailedFiles(new ArrayList<>());
        result.setProcessedAt(LocalDateTime.now());
        
        List<Document> allDocuments = new ArrayList<>();
        StringBuilder combinedContent = new StringBuilder();
        
        for (MultipartFile file : files) {            try {
                List<Document> documents = processFile(file);
                log.info("文件 {} 處理結果: {} 個文檔片段", file.getOriginalFilename(), documents.size());
                
                if (!documents.isEmpty()) {
                    allDocuments.addAll(documents);

                    // 記錄成功處理的文件
                    FileProcessingResult.ProcessedFile processedFile = new FileProcessingResult.ProcessedFile();
                    processedFile.setFileName(file.getOriginalFilename());
                    processedFile.setFileType(getFileType(file.getOriginalFilename()));
                    processedFile.setFileSize(file.getSize());
                    processedFile.setDocumentCount(documents.size());

                    // 合併文檔內容
                    String fileContent = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));
                    processedFile.setContent(fileContent);
                    processedFile.setContentLength(fileContent.length());
                    processedFile.setMetadata(extractFileMetadata(documents));
                    
                    result.getProcessedFiles().add(processedFile);

                    // 添加到組合內容
                    combinedContent.append("\n\n=== 文件: ").append(file.getOriginalFilename()).append(" ===\n");
                    for (Document doc : documents) {
                        combinedContent.append(doc.getText()).append("\n");
                    }
                    
                    log.info("成功處理文件: {}, 生成 {} 個文檔片段", 
                        file.getOriginalFilename(), documents.size());
                } else {
                    log.warn("文件 {} 處理後沒有生成任何文檔片段，將作為失敗文件處理", file.getOriginalFilename());
                    
                    // 將沒有內容的文件記錄為失敗
                    FileProcessingResult.FailedFile failedFile = new FileProcessingResult.FailedFile();
                    failedFile.setFileName(file.getOriginalFilename());
                    failedFile.setErrorMessage("文件處理後沒有提取到任何文本內容");
                    failedFile.setFileType(getFileType(file.getOriginalFilename()));
                    
                    result.getFailedFiles().add(failedFile);
                }
            } catch (Exception e) {
                // 記錄處理失敗的文件
                FileProcessingResult.FailedFile failedFile = new FileProcessingResult.FailedFile();
                failedFile.setFileName(file.getOriginalFilename());
                failedFile.setErrorMessage(e.getMessage());
                failedFile.setFileType(getFileType(file.getOriginalFilename()));
                
                result.getFailedFiles().add(failedFile);
                
                log.error("處理文件失敗: {}", file.getOriginalFilename(), e);
            }
        }
        
        // 設置統計信息
        result.setTotalDocuments(allDocuments.size());
        result.setTotalContentLength(combinedContent.length());
        result.setCombinedContent(combinedContent.toString());
        result.setContentSummary(generateContentSummary(allDocuments));
        
        log.info("文件處理完成: 成功 {} 個, 失敗 {} 個, 總文檔片段 {}", 
            result.getProcessedFiles().size(), 
            result.getFailedFiles().size(), 
            result.getTotalDocuments());
        
        return result;
    }

    /**
     * 處理單個文件
     */
    private List<Document> processFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String fileType = getFileType(fileName);
        
        log.info("開始處理文件: {}, 類型: {}, 大小: {} bytes", fileName, fileType, file.getSize());

        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        
        // 根據文件類型選擇合適的 DocumentReader
        DocumentReader documentReader = createDocumentReader(fileType, resource);
        
        // 讀取文檔
        List<Document> documents = documentReader.read();
        log.info("DocumentReader 讀取到 {} 個原始文檔", documents.size());

        // 檢查原始文檔內容
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getText();
            log.info("原始文檔 {} 內容長度: {}, 內容前100字符: {}", 
                i, content != null ? content.length() : 0, 
                content != null && !content.isEmpty() ?
                    content.substring(0, Math.min(100, content.length())) : "空內容");
        }
        
        // 使用 TokenTextSplitter 分割長文檔
        if (!documents.isEmpty()) {
            List<Document> splitDocuments = textSplitter.apply(documents);
            log.info("TokenTextSplitter 分割後得到 {} 個文檔片段", splitDocuments.size());

            // 檢查分割後的文檔內容
            for (int i = 0; i < Math.min(3, splitDocuments.size()); i++) {
                Document doc = splitDocuments.get(i);
                String content = doc.getText();
                log.info("分割文檔 {} 內容長度: {}", i, content != null ? content.length() : 0);
            }
            
            documents = splitDocuments;
        } else {
            log.warn("DocumentReader 沒有讀取到任何文檔內容");
        }
        
        return documents;
    }

    /**
     * 根據文件類型創建對應的 DocumentReader
     */
    private DocumentReader createDocumentReader(String fileType, ByteArrayResource resource) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> new PagePdfDocumentReader(resource);
            case "docx", "doc", "pptx", "ppt" -> new TikaDocumentReader(resource);
            case "txt" -> new TextReader(resource);
            case "json" -> new JsonReader(resource);
            case "md", "markdown" -> new MarkdownDocumentReader(resource, 
                MarkdownDocumentReaderConfig.builder().build());
            default -> new TextReader(resource);
        };
    }

    /**
     * 從文件名獲取文件類型
     */
    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 提取文件元數據
     */
    private Map<String, Object> extractFileMetadata(List<Document> documents) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (!documents.isEmpty()) {
            Document firstDoc = documents.getFirst();
            metadata.putAll(firstDoc.getMetadata());
        }
        
        metadata.put("document_count", documents.size());
        metadata.put("processed_at", LocalDateTime.now().toString());
        
        return metadata;
    }

    /**
     * 生成內容摘要
     */
    private String generateContentSummary(List<Document> documents) {
        if (documents.isEmpty()) {
            return "無可用內容";
        }
          int totalDocs = documents.size();
        long totalLength = documents.stream()
            .mapToLong(doc -> doc.getText() != null ? doc.getText().length() : 0)
            .sum();
        
        String contentPreview = documents.stream()
            .limit(3)  // 只取前3個文檔的開頭
            .filter(doc -> doc.getText() != null)
            .map(doc -> doc.getText().substring(0, Math.min(100, doc.getText().length())))
            .collect(Collectors.joining("... ", "", "..."));
        
        return String.format("共 %d 個文檔片段，總長度 %d 字符。內容預覽：%s", 
            totalDocs, totalLength, contentPreview);
    }
}

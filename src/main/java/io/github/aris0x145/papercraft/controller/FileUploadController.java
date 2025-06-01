package io.github.aris0x145.papercraft.controller;

import io.github.aris0x145.papercraft.model.FileProcessingResult;
import io.github.aris0x145.papercraft.model.FileUploadRequest;
import io.github.aris0x145.papercraft.model.PaperRequest;
import io.github.aris0x145.papercraft.service.FileProcessingService;
import io.github.aris0x145.papercraft.service.PaperGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 文件上傳控制器
 * 處理文件上傳和基於文件內容的報告生成
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileProcessingService fileProcessingService;
    private final PaperGenerationService paperGenerationService;

    /**
     * 文件上傳接口
     */
    @PostMapping("/upload")
    public ResponseEntity<FileProcessingResult> uploadFiles(
            @RequestParam("files") List<MultipartFile> files) {
        
        log.info("接收到文件上傳請求，文件數量: {}", files.size());
        
        // 驗證文件
        if (files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // 檢查文件大小限制 (例如: 10MB per file)
        for (MultipartFile file : files) {
            if (file.getSize() > 10 * 1024 * 1024) {
                log.warn("文件過大: {} ({}MB)", file.getOriginalFilename(), file.getSize() / 1024 / 1024);
                return ResponseEntity.badRequest().build();
            }
        }
        
        try {
            FileProcessingResult result = fileProcessingService.processFiles(files);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("文件處理失敗", e);
            return ResponseEntity.internalServerError().build();
        }
    }    /**
     * 基於上傳文件生成報告（流式）
     */
    @PostMapping(value = "/generate-report", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generateReportFromFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("topic") String topic,
            @RequestParam(value = "type", defaultValue = "research") String type,
            @RequestParam(value = "field", required = false) String field,
            @RequestParam(value = "wordCount", defaultValue = "3000") Integer wordCount,
            @RequestParam(value = "requirements", required = false) String requirements) {
        
        log.info("開始基於 {} 個文件生成論文: {}", files.size(), topic);
        
        try {
            // 1. 處理文件，提取內容
            FileProcessingResult processingResult = fileProcessingService.processFiles(files);
            
            if (processingResult.getProcessedFiles().isEmpty()) {
                return Flux.just("錯誤：沒有成功處理的文件，無法生成論文。\n");
            }
            
            // 2. 構建論文生成請求
            PaperRequest paperRequest = new PaperRequest();
            paperRequest.setTopic(topic);
            paperRequest.setType(type);
            paperRequest.setField(field);
            paperRequest.setWordCount(wordCount);
            paperRequest.setRequirements(requirements);
            
            // 3. 提取文件內容
            List<String> fileContents = processingResult.getProcessedFiles().stream()
                .map(FileProcessingResult.ProcessedFile::getContent)
                .toList();
            
            // 4. 使用增強的 PaperGenerationService 生成論文
            return paperGenerationService.generatePaperStream(paperRequest, fileContents);
            
        } catch (Exception e) {
            log.error("基於文件生成論文失敗", e);
            return Flux.just("錯誤：論文生成失敗 - " + e.getMessage() + "\n");
        }
    }

    /**
     * 獲取支持的文件類型
     */
    @GetMapping("/supported-types")
    public ResponseEntity<List<String>> getSupportedFileTypes() {
        List<String> supportedTypes = List.of(
            "pdf", "txt", "docx", "doc", "pptx", "ppt", 
            "json", "md", "markdown"
        );
        return ResponseEntity.ok(supportedTypes);
    }

    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("文件上傳服務正常");
    }
}

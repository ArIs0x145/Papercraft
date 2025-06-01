package io.github.aris0x145.papercraft.controller;

import io.github.aris0x145.papercraft.model.FileProcessingResult;
import io.github.aris0x145.papercraft.model.PaperRequest;
import io.github.aris0x145.papercraft.service.FileProcessingService;
import io.github.aris0x145.papercraft.service.PaperGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 論文生成 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/papers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaperGenerationController {

    private final PaperGenerationService paperGenerationService;
    private final FileProcessingService fileProcessingService;

    /**
     * 統一的論文生成 API - 支援有文件和無文件兩種模式
     */
    @PostMapping(value = "/generate", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generatePaper(
            @RequestParam("topic") String topic,
            @RequestParam(value = "type", defaultValue = "研究論文") String type,
            @RequestParam(value = "field", required = false) String field,
            @RequestParam(value = "language", defaultValue = "繁體中文") String language,
            @RequestParam(value = "requirements", required = false) String requirements,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        
        log.info("接收到論文生成請求 - 主題: {}, 文件數量: {}", topic, files != null ? files.size() : 0);
        
        try {
            // 構建論文生成請求
            PaperRequest paperRequest = new PaperRequest();
            paperRequest.setTopic(topic);
            paperRequest.setType(type);
            paperRequest.setField(field);
            paperRequest.setLanguage(language);
            paperRequest.setRequirements(requirements);
            
            // 根據是否有文件來決定處理方式
            if (files != null && !files.isEmpty()) {
                // 有文件：先處理文件，再生成論文
                log.info("基於 {} 個文件生成論文", files.size());
                
                FileProcessingResult processingResult = fileProcessingService.processFiles(files);
                
                if (processingResult.getProcessedFiles().isEmpty()) {
                    return Flux.just("錯誤：沒有成功處理的文件，無法生成論文。\n");
                }
                
                List<String> fileContents = processingResult.getProcessedFiles().stream()
                    .map(FileProcessingResult.ProcessedFile::getContent)
                    .toList();
                
                return paperGenerationService.generatePaperStream(paperRequest, fileContents);
            } else {
                // 無文件：直接生成論文
                log.info("直接生成論文（無文件）");
                return paperGenerationService.generatePaperStream(paperRequest);
            }
            
        } catch (Exception e) {
            log.error("論文生成失敗", e);
            return Flux.just("錯誤：論文生成失敗 - " + e.getMessage() + "\n");
        }
    }

    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public Flux<String> health() {
        return Flux.just("論文生成服務運行正常");
    }
}
    

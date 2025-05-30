package io.github.aris0x145.papercraft.controller;

import io.github.aris0x145.papercraft.model.PaperRequest;
import io.github.aris0x145.papercraft.model.PaperResult;
import io.github.aris0x145.papercraft.service.PaperGenerationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * 論文生成 REST API 控制器
 */

@Slf4j
@RestController
@RequestMapping("/api/papers")
@CrossOrigin(origins = "*")
public class PaperGenerationController {

    @Autowired(required = false)
    private PaperGenerationService paperGenerationService;

    /**
     * 開始生成論文
     */
    @PostMapping("/generate")
    public Mono<ResponseEntity<GenerationResponse>> generatePaper(@RequestBody PaperRequest request) {
        log.info("接收到論文生成請求: {}", request.getTopic());

        Mono<String> generationMono = paperGenerationService.startGeneration(request);

        return generationMono
            .map(taskId -> ResponseEntity.ok(new GenerationResponse(taskId, "論文生成已開始")))
            .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * 查詢生成進度
     */
    @GetMapping("/progress/{taskId}")
    public Mono<ResponseEntity<PaperResult>> getProgress(@PathVariable String taskId) {
        log.info("查詢生成進度: {}", taskId);

        Mono<PaperResult> progressMono = paperGenerationService.getProgress(taskId);
        
        return progressMono
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * 獲取完成的論文
     */
    @GetMapping("/result/{taskId}")
    public Mono<ResponseEntity<PaperResult>> getResult(@PathVariable String taskId) {
        log.info("獲取論文結果: {}", taskId);
        
        Mono<PaperResult> resultMono = paperGenerationService.getResult(taskId);

        return resultMono
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("論文生成服務運行正常"));
    }

    /**
     * 流式生成論文（Server-Sent Events）
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generatePaperStream(@RequestBody PaperRequest request) {
        log.info("接收到流式論文生成請求: {}", request.getTopic());

        return paperGenerationService.generatePaperStream(request)
            .map(chunk -> "data: " + chunk.replace("\n", "\\n") + "\n\n")
            .onErrorResume(error -> {
                log.error("流式生成出錯", error);
                return Flux.just("data: [ERROR] " + error.getMessage() + "\n\n");
            });
    }

    /**
     * 流式生成論文（純文本流）
     */
    @PostMapping(value = "/generate/stream-text", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generatePaperStreamText(@RequestBody PaperRequest request) {
        log.info("接收到純文本流式論文生成請求: {}", request.getTopic());

        return paperGenerationService.generatePaperStream(request)
            .onErrorResume(error -> {
                log.error("流式生成出錯", error);
                return Flux.just("\n[ERROR] " + error.getMessage() + "\n");
            });
    }

    /**
     * 生成響應DTO
     */
    @Getter
    public static class GenerationResponse {
        private final String taskId;
        private final String message;

        public GenerationResponse(String taskId, String message) {
            this.taskId = taskId;
            this.message = message;
        }

    }
}

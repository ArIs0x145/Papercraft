package io.github.aris0x145.papercraft.controller;

import io.github.aris0x145.papercraft.model.PaperRequest;
import io.github.aris0x145.papercraft.service.PaperGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 論文生成 REST API 控制器
 */

@Slf4j
@RestController
@RequestMapping("/api/papers")
@CrossOrigin(origins = "*")
public class PaperGenerationController {    @Autowired(required = false)
    private PaperGenerationService paperGenerationService;    /**
     * 流式生成論文
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> generatePaperStream(@RequestBody PaperRequest request) {
        log.info("接收到流式論文生成請求: {}", request.getTopic());

        return paperGenerationService.generatePaperStream(request)
            .onErrorResume(error -> {
                log.error("流式生成論文失敗", error);
                return Flux.just("錯誤: " + error.getMessage());
            });
    }    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public Flux<String> health() {
        return Flux.just("論文生成服務運行正常 - 僅支援流式生成");
    }
}

package org.ruoyi.system.controller.oj;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.service.oj.IAcAiAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 分析接口（用户端，SSE 流式）
 * 认证：token 通过 query param 传递，SecurityConfig 已对 /ac/** 做 StpUserUtil 校验
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ac/ai")
public class AcAiController {

    private final IAcAiAnalysisService aiAnalysisService;

    /**
     * 流式返回 AI 分析结果
     * GET /ac/ai/analyze/{submitId}
     * produces: text/event-stream
     */
    @GetMapping(value = "/analyze/{submitId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> analyze(@PathVariable Integer submitId) {
        return aiAnalysisService.streamAnalysis(submitId)
            .onErrorReturn("AI 分析出现异常，请稍后重试。");
    }
}

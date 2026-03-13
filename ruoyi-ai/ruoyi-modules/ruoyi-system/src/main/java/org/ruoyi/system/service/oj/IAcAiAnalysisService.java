package org.ruoyi.system.service.oj;

import reactor.core.publisher.Flux;

/**
 * AI 分析 Service 接口
 * @author 32846
 */
public interface IAcAiAnalysisService {

    /**
     * 流式返回 AI 分析结果
     * @param submitId 提交记录 ID
     * @return token 流
     */
    Flux<String> streamAnalysis(Integer submitId);
}

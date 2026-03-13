package org.ruoyi.system.service.oj.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.AcSubmit;
import org.ruoyi.system.mapper.AcSubmitMapper;
import org.ruoyi.system.service.IAcProblemService;
import org.ruoyi.system.service.oj.IAcAiAnalysisService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 分析 Service 实现
 * 根据判题结果选择对应 Prompt：
 *   result=2(AC)  → 代码优化建议
 *   result=3~7    → 错误诊断
 * @author 32846
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcAiAnalysisServiceImpl implements IAcAiAnalysisService {

    private final ChatClient chatClient;
    private final AcSubmitMapper submitMapper;
    private final IAcProblemService problemService;

    /** 判题结果文字映射 */
    private static final String[] RESULT_TEXT = {
        "PENDING", "JUDGING", "通过(AC)", "答案错误(WA)",
        "超时(TLE)", "超内存(MLE)", "运行错误(RE)", "编译错误(CE)"
    };

    @Override
    public Flux<String> streamAnalysis(Integer submitId) {
        AcSubmit submit = submitMapper.selectById(submitId);
        if (submit == null) {
            return Flux.error(new ServiceException("提交记录不存在"));
        }
        if (submit.getResult() == null || submit.getResult() <= 1) {
            return Flux.error(new ServiceException("判题尚未完成"));
        }

        AcProblem problem = problemService.getById(submit.getProblemId());
        if (problem == null) {
            return Flux.error(new ServiceException("题目不存在"));
        }

        String prompt = buildPrompt(submit, problem);
        log.info("AI 分析开始，submitId={}, result={}", submitId, submit.getResult());

        StringBuilder collector = new StringBuilder();

        return chatClient.prompt()
            .user(prompt)
            .stream()
            .content()
            .doOnNext(collector::append)
            .doOnComplete(() -> {
                // 流结束后，将完整 AI 分析结果保存到数据库
                try {
                    AcSubmit update = new AcSubmit();
                    update.setId(submitId);
                    update.setAiAnalysis(collector.toString());
                    submitMapper.updateById(update);
                    log.info("AI 分析结果已保存，submitId={}, length={}", submitId, collector.length());
                } catch (Exception e) {
                    log.error("保存 AI 分析结果失败，submitId={}", submitId, e);
                }
            });
    }

    private String buildPrompt(AcSubmit submit, AcProblem problem) {
        if (submit.getResult() == 2) {
            // AC → 代码优化建议
            return String.format("""
                你是一个编程教学助手。用户提交了以下代码并通过了所有测试用例。
                题目：%s
                用户代码：
                ```%s
                %s
                ```
                请分析代码的时间和空间复杂度，并给出优化建议。如果有更优的解法思路，请简要说明。用中文回答。
                """,
                problem.getDescription(),
                submit.getLanguage(),
                submit.getCode()
            );
        } else {
            // WA/TLE/MLE/RE/CE → 错误诊断
            String resultText = submit.getResult() < RESULT_TEXT.length
                ? RESULT_TEXT[submit.getResult()] : "未知错误";
            return String.format("""
                你是一个编程教学助手。用户提交了以下代码解决一道编程题，但结果不正确。
                题目：%s
                用户代码：
                ```%s
                %s
                ```
                编程语言：%s
                判题结果：%s
                错误信息：%s
                请分析错误原因，给出具体的修复思路。用中文回答，语言简洁易懂。
                """,
                problem.getDescription(),
                submit.getLanguage(),
                submit.getCode(),
                submit.getLanguage(),
                resultText,
                submit.getErrorLog() != null ? submit.getErrorLog() : "无"
            );
        }
    }
}

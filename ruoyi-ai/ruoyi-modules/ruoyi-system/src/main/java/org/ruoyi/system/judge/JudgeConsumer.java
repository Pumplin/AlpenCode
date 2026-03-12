package org.ruoyi.system.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.AcSubmit;
import org.ruoyi.system.domain.AcTestCase;
import org.ruoyi.system.judge.config.RabbitMQConfig;
import org.ruoyi.system.mapper.AcSubmitMapper;
import org.ruoyi.system.mapper.AcTestCaseMapper;
import org.ruoyi.system.service.IAcProblemService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 判题 MQ 消费者
 * @author 32846
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeConsumer {

    private final AcSubmitMapper submitMapper;
    private final AcTestCaseMapper testCaseMapper;
    private final IAcProblemService problemService;
    private final DockerSandbox dockerSandbox;

    @RabbitListener(queues = RabbitMQConfig.JUDGE_QUEUE)
    public void onJudgeMessage(Integer submitId, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到判题任务，submitId={}", submitId);
        try {
            doJudge(submitId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("判题异常，submitId={}", submitId, e);
            try {
                // 更新为 RE
                updateResult(submitId, 6, 0, 0, 0, e.getMessage());
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("ACK 失败", ex);
            }
        }
    }

    private void doJudge(Integer submitId) {
        AcSubmit submit = submitMapper.selectById(submitId);
        if (submit == null) {
            log.warn("提交记录不存在，submitId={}", submitId);
            return;
        }

        // 更新为 JUDGING
        submit.setResult(1);
        submitMapper.updateById(submit);

        AcProblem problem = problemService.getById(submit.getProblemId());
        if (problem == null || problem.getMetaData() == null) {
            updateResult(submitId, 6, 0, 0, 0, "题目或 metaData 不存在");
            return;
        }

        // 取全部测试用例
        List<AcTestCase> testCases = testCaseMapper.selectList(
            new LambdaQueryWrapper<AcTestCase>()
                .eq(AcTestCase::getProblemId, submit.getProblemId())
                .eq(AcTestCase::getStatus, 0)
                .orderByAsc(AcTestCase::getSort)
        );

        if (testCases.isEmpty()) {
            updateResult(submitId, 6, 0, 0, 0, "该题目没有测试用例");
            return;
        }

        String driverCode = DriverCodeGenerator.generate(problem.getMetaData(), submit.getLanguage());
        List<String> inputs = testCases.stream().map(AcTestCase::getInput).toList();

        // 批量执行：一个容器跑完所有用例
        DockerSandbox.ExecResult batchResult;
        long start = System.currentTimeMillis();
        try {
            batchResult = switch (submit.getLanguage().toLowerCase()) {
                case "java" -> dockerSandbox.runJavaBatch(submit.getCode(), driverCode, inputs, problem.getTimeLimit());
                case "python3", "python" -> dockerSandbox.runPythonBatch(submit.getCode(), driverCode, inputs, problem.getTimeLimit());
                default -> throw new IllegalArgumentException("不支持的语言: " + submit.getLanguage());
            };
        } catch (Exception e) {
            updateResult(submitId, 6, 0, testCases.size(), 0, e.getMessage());
            return;
        }

        int totalTimeCost = (int) (System.currentTimeMillis() - start);

        // 容器整体失败（编译错误等）
        if (!batchResult.success() && batchResult.stdout().isEmpty()) {
            String err = batchResult.stderr().isEmpty() ? batchResult.stdout() : batchResult.stderr();
            int errResult;
            if (err.contains("javac") || err.contains("SyntaxError") || err.contains("error:")) {
                errResult = 7; // CE
            } else if (err.contains("Time Limit Exceeded") || err.contains("timeout")) {
                errResult = 4; // TLE
            } else {
                errResult = 6; // RE
            }
            updateResult(submitId, errResult, 0, testCases.size(), totalTimeCost / testCases.size(), err);
            return;
        }

        // 按 CASE_END 分割输出，逐一对比
        String[] caseOutputs = batchResult.stdout().split(DriverCodeGenerator.CASE_END, -1);
        int passCount = 0;
        int result = 2; // AC
        String errorLog = null;

        for (int i = 0; i < testCases.size(); i++) {
            AcTestCase tc = testCases.get(i);
            if (i >= caseOutputs.length) {
                // 输出段数不够，中途崩了
                result = 6; // RE
                errorLog = batchResult.stderr().isEmpty() ? "执行中断" : batchResult.stderr();
                break;
            }
            String output = caseOutputs[i].trim();
            if (output.equals(tc.getExpectedOutput().trim())) {
                passCount++;
            } else {
                result = 3; // WA
                errorLog = "期望: " + tc.getExpectedOutput().trim() + "\n实际: " + output;
                break;
            }
        }

        int avgTimeCost = totalTimeCost / testCases.size();
        updateResult(submitId, result, passCount, testCases.size(), avgTimeCost, errorLog);

        // 如果 AC，更新题目通过次数
        if (result == 2) {
            problemService.lambdaUpdate()
                .eq(AcProblem::getId, submit.getProblemId())
                .setSql("ac_count = ac_count + 1")
                .update();
        }
        // 更新提交次数
        problemService.lambdaUpdate()
            .eq(AcProblem::getId, submit.getProblemId())
            .setSql("submit_count = submit_count + 1")
            .update();

        log.info("判题完成，submitId={}, result={}, pass={}/{}", submitId, result, passCount, testCases.size());
    }


    private void updateResult(Integer submitId, int result, int passCount, int totalCount, int timeCost, String errorLog) {
        AcSubmit update = new AcSubmit();
        update.setId(submitId);
        update.setResult(result);
        update.setPassCount(passCount);
        update.setTotalCount(totalCount);
        update.setTimeCost(timeCost);
        update.setErrorLog(errorLog);
        submitMapper.updateById(update);
    }
}

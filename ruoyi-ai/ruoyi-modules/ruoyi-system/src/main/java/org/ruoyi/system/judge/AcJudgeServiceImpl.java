package org.ruoyi.system.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.AcSubmit;
import org.ruoyi.system.domain.AcTestCase;
import org.ruoyi.system.domain.dto.JudgeRequestDTO;
import org.ruoyi.system.domain.vo.RunCodeResultVO;
import org.ruoyi.system.judge.config.RabbitMQConfig;
import org.ruoyi.system.mapper.AcSubmitMapper;
import org.ruoyi.system.mapper.AcTestCaseMapper;
import org.ruoyi.system.service.IAcProblemService;
import org.ruoyi.system.service.oj.IAcJudgeService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 判题 Service 实现
 * 一个容器跑完所有测试用例，避免每个用例单独起容器的开销。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcJudgeServiceImpl implements IAcJudgeService {

    private final IAcProblemService problemService;
    private final AcTestCaseMapper testCaseMapper;
    private final AcSubmitMapper submitMapper;
    private final DockerSandbox dockerSandbox;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public List<RunCodeResultVO> runCode(JudgeRequestDTO dto) {
        AcProblem problem = getProblem(dto.getProblemId());

        List<AcTestCase> samples = testCaseMapper.selectList(
            new LambdaQueryWrapper<AcTestCase>()
                .eq(AcTestCase::getProblemId, dto.getProblemId())
                .eq(AcTestCase::getIsSample, 1)
                .eq(AcTestCase::getStatus, 0)
                .orderByAsc(AcTestCase::getSort)
        );

        if (samples.isEmpty()) {
            throw new ServiceException("该题目暂无公开样例");
        }

        String driverCode = DriverCodeGenerator.generate(problem.getMetaData(), dto.getLanguage());
        return executeBatch(dto, problem, driverCode, samples);
    }

    @Override
    public Integer submit(JudgeRequestDTO dto, Integer userId) {
        AcProblem problem = getProblem(dto.getProblemId());

        long totalCount = testCaseMapper.selectCount(
            new LambdaQueryWrapper<AcTestCase>()
                .eq(AcTestCase::getProblemId, dto.getProblemId())
                .eq(AcTestCase::getStatus, 0)
        );

        AcSubmit submit = new AcSubmit();
        submit.setUserId(userId);
        submit.setProblemId(dto.getProblemId());
        submit.setLanguage(dto.getLanguage());
        submit.setCode(dto.getCode());
        submit.setResult(0); // PENDING
        submit.setTotalCount((int) totalCount);
        submit.setPassCount(0);
        submit.setIsDelete(CommonConstants.NOT_DELETE);
        submitMapper.insert(submit);

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.JUDGE_EXCHANGE,
            RabbitMQConfig.JUDGE_ROUTING_KEY,
            submit.getId()
        );

        log.info("提交判题任务，submitId={}, problemId={}", submit.getId(), dto.getProblemId());
        return submit.getId();
    }

    /**
     * 批量执行：一个容器跑完所有用例，解析输出后逐一对比
     */
    private List<RunCodeResultVO> executeBatch(JudgeRequestDTO dto, AcProblem problem,
                                                String driverCode, List<AcTestCase> testCases) {
        List<String> inputs = testCases.stream().map(AcTestCase::getInput).toList();

        long start = System.currentTimeMillis();
        DockerSandbox.ExecResult batchResult;
        try {
            batchResult = switch (dto.getLanguage().toLowerCase()) {
                case "java" -> dockerSandbox.runJavaBatch(dto.getCode(), driverCode, inputs, problem.getTimeLimit());
                case "python3", "python" -> dockerSandbox.runPythonBatch(dto.getCode(), driverCode, inputs, problem.getTimeLimit());
                default -> throw new ServiceException("不支持的语言: " + dto.getLanguage());
            };
        } catch (Exception e) {
            // 容器启动失败，所有用例标记失败
            List<RunCodeResultVO> results = new ArrayList<>();
            for (AcTestCase tc : testCases) {
                RunCodeResultVO vo = new RunCodeResultVO();
                vo.setInput(tc.getInput());
                vo.setExpectedOutput(tc.getExpectedOutput().trim());
                vo.setPassed(false);
                vo.setErrorLog(e.getMessage());
                vo.setTimeCost(0);
                results.add(vo);
            }
            return results;
        }

        int totalTimeCost = (int) (System.currentTimeMillis() - start);

        // 如果容器整体失败（编译错误等），所有用例标记失败
        if (!batchResult.success() && batchResult.stdout().isEmpty()) {
            List<RunCodeResultVO> results = new ArrayList<>();
            String errorMsg = batchResult.stderr().isEmpty() ? batchResult.stdout() : batchResult.stderr();
            for (AcTestCase tc : testCases) {
                RunCodeResultVO vo = new RunCodeResultVO();
                vo.setInput(tc.getInput());
                vo.setExpectedOutput(tc.getExpectedOutput().trim());
                vo.setPassed(false);
                vo.setErrorLog(errorMsg);
                vo.setTimeCost(totalTimeCost / testCases.size());
                results.add(vo);
            }
            return results;
        }

        // 按 CASE_END 分割输出，每段对应一个用例
        String[] caseOutputs = batchResult.stdout().split(DriverCodeGenerator.CASE_END, -1);
        int avgTimeCost = totalTimeCost / testCases.size();

        List<RunCodeResultVO> results = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            AcTestCase tc = testCases.get(i);
            RunCodeResultVO vo = new RunCodeResultVO();
            vo.setInput(tc.getInput());
            vo.setExpectedOutput(tc.getExpectedOutput().trim());
            vo.setTimeCost(avgTimeCost);

            if (i >= caseOutputs.length) {
                // 输出段数不够，说明中途崩了
                vo.setPassed(false);
                vo.setErrorLog(batchResult.stderr().isEmpty() ? "执行中断" : batchResult.stderr());
            } else {
                String output = caseOutputs[i].trim();
                vo.setOutput(output);
                vo.setPassed(output.equals(tc.getExpectedOutput().trim()));
                if (!vo.getPassed() && !batchResult.stderr().isEmpty()) {
                    vo.setErrorLog(batchResult.stderr());
                }
            }
            results.add(vo);
        }
        return results;
    }

    private AcProblem getProblem(Integer problemId) {
        AcProblem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new ServiceException("题目不存在");
        }
        if (problem.getMetaData() == null || problem.getMetaData().isBlank()) {
            throw new ServiceException("题目缺少 metaData，无法生成 Driver 代码");
        }
        return problem;
    }
}

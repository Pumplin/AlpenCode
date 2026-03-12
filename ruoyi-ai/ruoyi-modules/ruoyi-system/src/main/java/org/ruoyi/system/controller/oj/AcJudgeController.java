package org.ruoyi.system.controller.oj;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.system.domain.dto.JudgeRequestDTO;
import org.ruoyi.system.domain.vo.RunCodeResultVO;
import org.ruoyi.system.service.oj.IAcJudgeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 判题接口（用户端）
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ac/judge")
public class AcJudgeController {

    private final IAcJudgeService judgeService;

    /**
     * Run Code：运行公开样例，同步返回结果，不记录提交
     */
    @PostMapping("/run")
    public R<List<RunCodeResultVO>> runCode(@Valid @RequestBody JudgeRequestDTO dto) {
        return R.ok(judgeService.runCode(dto));
    }

    /**
     * Submit：提交代码，异步判题，返回 submitId 供前端轮询
     */
    @PostMapping("/submit")
    public R<Map<String, Integer>> submit(@Valid @RequestBody JudgeRequestDTO dto) {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        Integer submitId = judgeService.submit(dto, userId);
        return R.ok(Map.of("submitId", submitId));
    }
}

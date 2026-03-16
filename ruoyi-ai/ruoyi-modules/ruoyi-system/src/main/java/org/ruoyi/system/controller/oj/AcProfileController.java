package org.ruoyi.system.controller.oj;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.system.domain.dto.AcPasswordUpdateDTO;
import org.ruoyi.system.domain.dto.AcUserDTO;
import org.ruoyi.system.domain.vo.AcAiReportVo;
import org.ruoyi.system.domain.vo.AcUserStatsVo;
import org.ruoyi.system.service.oj.IAcProfileService;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心控制器（用户端）
 *
 * @author 32846
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ac/profile")
public class AcProfileController {

    private final IAcProfileService profileService;

    /**
     * 修改用户信息
     */
    @PutMapping("/info")
    public R<Void> updateInfo(@Valid @RequestBody AcUserDTO dto) {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        profileService.updateInfo(userId, dto);
        return R.ok();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public R<Void> updatePassword(@Valid @RequestBody AcPasswordUpdateDTO dto) {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        profileService.updatePassword(userId, dto);
        return R.ok();
    }

    /**
     * 获取用户刷题统计数据
     */
    @GetMapping("/stats")
    public R<AcUserStatsVo> getStats() {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        return R.ok(profileService.getStats(userId));
    }

    /**
     * 触发生成 AI 报告
     */
    @PostMapping("/report/generate")
    public R<Integer> generateReport() {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        return R.ok(profileService.generateReport(userId));
    }

    /**
     * 查询最新报告
     */
    @GetMapping("/report/latest")
    public R<AcAiReportVo> getLatestReport() {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        return R.ok(profileService.getLatestReport(userId));
    }
}

package org.ruoyi.system.service.oj;

import org.ruoyi.system.domain.dto.AcPasswordUpdateDTO;
import org.ruoyi.system.domain.dto.AcUserDTO;
import org.ruoyi.system.domain.vo.AcAiReportVo;
import org.ruoyi.system.domain.vo.AcUserStatsVo;

/**
 * 个人中心Service接口
 *
 * @author 32846
 */
public interface IAcProfileService {

    /**
     * 修改用户信息（复用 AcUserDTO）
     *
     * @param userId 用户ID
     * @param dto    用户信息
     */
    void updateInfo(Integer userId, AcUserDTO dto);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param dto    密码修改信息
     */
    void updatePassword(Integer userId, AcPasswordUpdateDTO dto);

    /**
     * 获取用户刷题统计数据
     *
     * @param userId 用户ID
     * @return 统计数据
     */
    AcUserStatsVo getStats(Integer userId);

    /**
     * 触发生成 AI 报告（异步）
     *
     * @param userId 用户ID
     * @return 报告ID
     */
    Integer generateReport(Integer userId);

    /**
     * 查询最新报告
     *
     * @param userId 用户ID
     * @return 报告视图
     */
    AcAiReportVo getLatestReport(Integer userId);
}

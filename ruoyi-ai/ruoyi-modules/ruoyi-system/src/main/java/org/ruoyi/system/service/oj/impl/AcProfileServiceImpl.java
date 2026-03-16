package org.ruoyi.system.service.oj.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.AcAiReport;
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.AcProblemCategory;
import org.ruoyi.system.domain.AcProblemCategoryMap;
import org.ruoyi.system.domain.AcSubmit;
import org.ruoyi.system.domain.AcUser;
import org.ruoyi.system.domain.dto.AcPasswordUpdateDTO;
import org.ruoyi.system.domain.dto.AcUserDTO;
import org.ruoyi.system.domain.vo.AcAiReportVo;
import org.ruoyi.system.domain.vo.AcUserStatsVo;
import org.ruoyi.system.mapper.AcAiReportMapper;
import org.ruoyi.system.mapper.AcProblemCategoryMapMapper;
import org.ruoyi.system.mapper.AcProblemCategoryMapper;
import org.ruoyi.system.mapper.AcProblemMapper;
import org.ruoyi.system.mapper.AcSubmitMapper;
import org.ruoyi.system.mapper.AcUserMapper;
import org.ruoyi.system.service.IAcUserService;
import org.ruoyi.system.service.oj.IAcProfileService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 个人中心Service实现
 *
 * @author 32846
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcProfileServiceImpl implements IAcProfileService {

    private final IAcUserService acUserService;
    private final AcUserMapper acUserMapper;
    private final AcSubmitMapper acSubmitMapper;
    private final AcAiReportMapper acAiReportMapper;
    private final AcProblemMapper acProblemMapper;
    private final AcProblemCategoryMapMapper acProblemCategoryMapMapper;
    private final AcProblemCategoryMapper acProblemCategoryMapper;
    private final ChatClient chatClient;

    @Override
    public void updateInfo(Integer userId, AcUserDTO dto) {
        // 校验用户名唯一性（排除自身）
        Long count = acUserMapper.selectCount(
            Wrappers.<AcUser>lambdaQuery()
                .eq(AcUser::getUsername, dto.getUsername())
                .ne(AcUser::getId, userId)
        );
        if (count > 0) {
            throw new ServiceException("用户名已存在");
        }
        dto.setId(userId);
        acUserService.updateByDTO(dto);
    }

    @Override
    public void updatePassword(Integer userId, AcPasswordUpdateDTO dto) {
        AcUser user = acUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        // 校验旧密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPasswordHash())) {
            throw new ServiceException("旧密码错误");
        }
        // 哈希新密码并更新
        String newHash = BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt());
        AcUser update = new AcUser();
        update.setId(userId);
        update.setPasswordHash(newHash);
        acUserMapper.updateById(update);
    }

    @Override
    public AcUserStatsVo getStats(Integer userId) {
        AcUserStatsVo stats = new AcUserStatsVo();

        // 1. 总提交次数
        Long totalSubmissions = acSubmitMapper.selectCount(
            Wrappers.<AcSubmit>lambdaQuery().eq(AcSubmit::getUserId, userId)
        );
        stats.setTotalSubmissions(totalSubmissions.intValue());

        // 2. 通过题目（去重 problemId where result=2）
        List<AcSubmit> acSubmits = acSubmitMapper.selectList(
            Wrappers.<AcSubmit>lambdaQuery()
                .select(AcSubmit::getProblemId)
                .eq(AcSubmit::getUserId, userId)
                .eq(AcSubmit::getResult, 2)
        );
        List<Integer> solvedProblemIds = acSubmits.stream()
            .map(AcSubmit::getProblemId)
            .distinct()
            .collect(Collectors.toList());
        stats.setSolvedCount(solvedProblemIds.size());

        // 3. 通过率
        double acceptRate = totalSubmissions > 0
            ? Math.round((double) solvedProblemIds.size() / totalSubmissions * 1000.0) / 10.0
            : 0.0;
        stats.setAcceptRate(acceptRate);

        // 4. 各难度通过数
        int easyCount = 0, mediumCount = 0, hardCount = 0;
        if (!solvedProblemIds.isEmpty()) {
            List<AcProblem> solvedProblems = acProblemMapper.selectList(
                Wrappers.<AcProblem>lambdaQuery()
                    .select(AcProblem::getDifficulty)
                    .in(AcProblem::getId, solvedProblemIds)
            );
            for (AcProblem p : solvedProblems) {
                if (p.getDifficulty() != null) {
                    switch (p.getDifficulty()) {
                        case 1 -> easyCount++;
                        case 2 -> mediumCount++;
                        case 3 -> hardCount++;
                    }
                }
            }
        }
        stats.setEasyCount(easyCount);
        stats.setMediumCount(mediumCount);
        stats.setHardCount(hardCount);

        // 5. 错误类型分布
        int waCount = 0, tleCount = 0, reCount = 0, ceCount = 0;
        List<AcSubmit> errorSubmits = acSubmitMapper.selectList(
            Wrappers.<AcSubmit>lambdaQuery()
                .select(AcSubmit::getResult)
                .eq(AcSubmit::getUserId, userId)
                .in(AcSubmit::getResult, List.of(3, 4, 6, 7))
        );
        for (AcSubmit s : errorSubmits) {
            switch (s.getResult()) {
                case 3 -> waCount++;
                case 4 -> tleCount++;
                case 6 -> reCount++;
                case 7 -> ceCount++;
            }
        }
        stats.setWaCount(waCount);
        stats.setTleCount(tleCount);
        stats.setReCount(reCount);
        stats.setCeCount(ceCount);

        // 6. 各分类通过数
        List<AcUserStatsVo.CategoryStat> categoryStats = new ArrayList<>();
        if (!solvedProblemIds.isEmpty()) {
            // 查询已通过题目的分类映射
            List<AcProblemCategoryMap> maps = acProblemCategoryMapMapper.selectList(
                Wrappers.<AcProblemCategoryMap>lambdaQuery()
                    .in(AcProblemCategoryMap::getProblemId, solvedProblemIds)
            );
            // 按 categoryId 分组，统计每个分类下的去重题目数
            Map<Integer, Long> categoryCountMap = maps.stream()
                .collect(Collectors.groupingBy(
                    AcProblemCategoryMap::getCategoryId,
                    Collectors.mapping(AcProblemCategoryMap::getProblemId, Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))
                ));
            if (!categoryCountMap.isEmpty()) {
                // 查询分类名称
                List<AcProblemCategory> categories = acProblemCategoryMapper.selectList(
                    Wrappers.<AcProblemCategory>lambdaQuery()
                        .in(AcProblemCategory::getId, categoryCountMap.keySet())
                );
                Map<Integer, String> categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(AcProblemCategory::getId, AcProblemCategory::getName));
                for (Map.Entry<Integer, Long> entry : categoryCountMap.entrySet()) {
                    AcUserStatsVo.CategoryStat cs = new AcUserStatsVo.CategoryStat();
                    cs.setName(categoryNameMap.getOrDefault(entry.getKey(), "未知分类"));
                    cs.setCount(entry.getValue().intValue());
                    categoryStats.add(cs);
                }
            }
        }
        stats.setCategoryStats(categoryStats);

        // 7. 注册天数
        AcUser user = acUserMapper.selectById(userId);
        if (user != null && user.getCreatedAt() != null) {
            long days = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
            stats.setDaysSinceJoin((int) Math.max(days, 1));
        } else {
            stats.setDaysSinceJoin(0);
        }

        return stats;
    }

    @Override
    public Integer generateReport(Integer userId) {
        // 1. 聚合统计数据
        AcUserStatsVo stats = getStats(userId);

        // 2. 插入报告记录（status=0 生成中）
        AcAiReport report = new AcAiReport();
        report.setUserId(userId);
        report.setStatus(0);
        report.setStatsSnapshot(JSONUtil.toJsonStr(stats));
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        acAiReportMapper.insert(report);
        Integer reportId = report.getId();

        // 3. 异步调用 AI 生成报告
        CompletableFuture.runAsync(() -> {
            try {
                String prompt = buildReportPrompt(stats);
                String content = chatClient.prompt().user(prompt).call().content();

                AcAiReport update = new AcAiReport();
                update.setId(reportId);
                update.setStatus(1);
                update.setReportContent(content);
                update.setUpdatedAt(LocalDateTime.now());
                acAiReportMapper.updateById(update);
                log.info("AI 报告生成成功，reportId={}", reportId);
            } catch (Exception e) {
                log.error("AI 报告生成失败，reportId={}", reportId, e);
                AcAiReport update = new AcAiReport();
                update.setId(reportId);
                update.setStatus(2);
                update.setUpdatedAt(LocalDateTime.now());
                acAiReportMapper.updateById(update);
            }
        });

        return reportId;
    }

    @Override
    public AcAiReportVo getLatestReport(Integer userId) {
        AcAiReport report = acAiReportMapper.selectOne(
            Wrappers.<AcAiReport>lambdaQuery()
                .eq(AcAiReport::getUserId, userId)
                .orderByDesc(AcAiReport::getCreatedAt)
                .last("LIMIT 1")
        );
        if (report == null) {
            return null;
        }
        AcAiReportVo vo = new AcAiReportVo();
        vo.setId(report.getId());
        vo.setStatus(report.getStatus());
        vo.setStatsSnapshot(report.getStatsSnapshot());
        vo.setReportContent(report.getReportContent());
        vo.setCreatedAt(report.getCreatedAt());
        return vo;
    }

    private String buildReportPrompt(AcUserStatsVo stats) {
        // 构建分类统计文本
        String categoryStatsText = "暂无数据";
        if (stats.getCategoryStats() != null && !stats.getCategoryStats().isEmpty()) {
            categoryStatsText = stats.getCategoryStats().stream()
                .sorted((a, b) -> b.getCount() - a.getCount())
                .map(cs -> cs.getName() + " " + cs.getCount() + "题")
                .collect(Collectors.joining("、"));
        }

        // 计算擅长/薄弱分类
        String strongTags = "暂无";
        String weakTags = "暂无";
        if (stats.getCategoryStats() != null && stats.getCategoryStats().size() >= 2) {
            List<AcUserStatsVo.CategoryStat> sorted = stats.getCategoryStats().stream()
                .sorted((a, b) -> b.getCount() - a.getCount())
                .collect(Collectors.toList());
            strongTags = sorted.subList(0, Math.min(3, sorted.size())).stream()
                .map(AcUserStatsVo.CategoryStat::getName).collect(Collectors.joining("、"));
            weakTags = sorted.subList(Math.max(0, sorted.size() - 3), sorted.size()).stream()
                .map(AcUserStatsVo.CategoryStat::getName).collect(Collectors.joining("、"));
        }

        return String.format("""
            你是一个编程学习分析师。以下是用户的刷题统计数据：
            注册天数：%d 天
            总提交：%d 次
            通过题目：%d 题
            通过率：%.1f%%
            各难度通过情况：简单 %d、中等 %d、困难 %d
            错误类型分布：WA %d次、TLE %d次、RE %d次、CE %d次
            各分类通过情况（从多到少）：%s
            擅长分类：%s
            薄弱分类：%s

            请生成一份详细的个性化能力分析报告，严格按以下 JSON 格式返回（不要包含 markdown 代码块标记）：
            {
              "overview": "总览描述，3-4句话，结合注册天数和通过率给出整体评价",
              "personalityTag": "一个4-6字的个性标签，如'稳扎稳打型'、'算法探索者'",
              "abilityLevel": "综合能力等级，从以下选一个：入门新手、初级选手、中级选手、高级选手、竞赛高手",
              "abilityScore": 75,
              "difficultyComment": "对各难度通过情况的深入分析，3-4句话，指出当前阶段和下一步方向",
              "difficultyAdvice": "针对难度提升的具体建议，1-2句话",
              "categoryComment": "对各分类能力的详细分析，3-4句话，指出擅长和薄弱项及原因",
              "strongCategories": ["擅长分类1", "擅长分类2"],
              "weakCategories": ["薄弱分类1", "薄弱分类2"],
              "categoryAdvice": "针对分类能力提升的具体建议，1-2句话",
              "errorComment": "对错误类型的深入分析，3-4句话，分析主要错误原因",
              "errorAdvice": "针对主要错误类型的改进方法，1-2句话",
              "summary": "综合能力评价，4-5句话，包含优势、不足和整体评价",
              "highlights": ["亮点1（如：连续刷题X天）", "亮点2", "亮点3"],
              "suggestions": ["具体可执行的建议1", "具体可执行的建议2", "具体可执行的建议3", "具体可执行的建议4"],
              "nextGoal": "下一阶段的具体目标，1句话"
            }
            用中文回答。abilityScore 是0-100的整数，根据通过率、题目数量、难度分布综合评估。
            """,
            stats.getDaysSinceJoin(),
            stats.getTotalSubmissions(),
            stats.getSolvedCount(),
            stats.getAcceptRate(),
            stats.getEasyCount(),
            stats.getMediumCount(),
            stats.getHardCount(),
            stats.getWaCount(),
            stats.getTleCount(),
            stats.getReCount(),
            stats.getCeCount(),
            categoryStatsText,
            strongTags,
            weakTags
        );
    }
}

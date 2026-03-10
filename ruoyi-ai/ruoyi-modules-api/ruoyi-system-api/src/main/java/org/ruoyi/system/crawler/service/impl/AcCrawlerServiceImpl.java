package org.ruoyi.system.crawler.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.crawler.client.LeetCodeApiClient;
import org.ruoyi.system.crawler.domain.dto.LeetCodeProblemDTO;
import org.ruoyi.system.crawler.domain.dto.TestCaseDTO;
import org.ruoyi.system.crawler.domain.vo.CrawlerResultVO;
import org.ruoyi.system.crawler.domain.vo.FailedProblemVO;
import org.ruoyi.system.crawler.mapper.DifficultyMapper;
import org.ruoyi.system.crawler.parser.TestCaseParser;
import org.ruoyi.system.crawler.service.IAcCrawlerService;
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.AcProblemCategory;
import org.ruoyi.system.domain.AcProblemCategoryMap;
import org.ruoyi.system.domain.AcTestCase;
import org.ruoyi.system.service.IAcProblemCategoryMapService;
import org.ruoyi.system.service.IAcProblemCategoryService;
import org.ruoyi.system.service.IAcProblemService;
import org.ruoyi.system.service.IAcTestCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * LeetCode 爬虫服务实现类
 * 
 * @author AlpenCode
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcCrawlerServiceImpl implements IAcCrawlerService {
    
    private final LeetCodeApiClient apiClient;
    private final TestCaseParser testCaseParser;
    private final DifficultyMapper difficultyMapper;
    private final IAcProblemService problemService;
    private final IAcProblemCategoryService categoryService;
    private final IAcProblemCategoryMapService categoryMapService;
    private final IAcTestCaseService testCaseService;
    
    @Override
    public CrawlerResultVO executeCrawl(Integer limit) {
        log.info("开始爬取 LeetCode 题目，数量: {}", limit);
        
        CrawlerResultVO result = new CrawlerResultVO();
        result.setSuccessCount(0);
        result.setSkipCount(0);
        result.setFailCount(0);
        result.setFailedProblems(new ArrayList<>());
        
        try {
            // 1. 获取题目列表
            List<LeetCodeProblemDTO> problemList = apiClient.fetchProblemList(limit);
            
            if (problemList.isEmpty()) {
                log.warn("LeetCode API 返回空题目列表");
                return result;
            }
            
            log.info("成功获取题目列表，数量: {}", problemList.size());
            
            // 2. 遍历处理每道题目
            for (LeetCodeProblemDTO leetcodeProblem : problemList) {
                try {
                    processSingleProblem(leetcodeProblem, result);
                } catch (Exception e) {
                    log.error("处理题目失败: {}", leetcodeProblem.getTitle(), e);
                    result.setFailCount(result.getFailCount() + 1);
                    
                    FailedProblemVO failedProblem = new FailedProblemVO();
                    failedProblem.setTitle(leetcodeProblem.getTitle());
                    failedProblem.setReason(e.getMessage());
                    result.getFailedProblems().add(failedProblem);
                }
            }
            
            log.info("爬取完成 - 成功: {}, 跳过: {}, 失败: {}",
                result.getSuccessCount(),
                result.getSkipCount(),
                result.getFailCount());
            
        } catch (Exception e) {
            log.error("爬取操作失败", e);
            throw new ServiceException("爬取失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 处理单道题目
     * 
     * @param leetcodeProblem LeetCode 题目数据
     * @param result 爬取结果对象
     * @throws InterruptedException 线程中断异常
     */
    @Transactional(rollbackFor = Exception.class)
    private void processSingleProblem(LeetCodeProblemDTO leetcodeProblem, 
                                      CrawlerResultVO result) 
            throws InterruptedException {
        
        // 1. 检查是否重复
        if (isDuplicate(leetcodeProblem.getTitle())) {
            log.info("题目已存在，跳过: {}", leetcodeProblem.getTitle());
            result.setSkipCount(result.getSkipCount() + 1);
            return;
        }
        
        // 2. 获取题目详情
        LeetCodeProblemDTO detail = apiClient.fetchProblemDetail(
            leetcodeProblem.getTitleSlug()
        );
        
        // 3. 解析示例测试用例
        List<TestCaseDTO> testCases = testCaseParser.parseExamples(
            detail.getDescription()
        );
        
        // 4. 映射并保存题目
        AcProblem problem = mapToProblem(detail);
        problemService.save(problem);
        
        // 5. 处理分类标签
        if (CollUtil.isNotEmpty(detail.getTags())) {
            processTags(problem.getId(), detail.getTags());
        }
        
        // 6. 保存示例测试用例
        if (CollUtil.isNotEmpty(testCases)) {
            saveTestCases(problem.getId(), testCases);
        }
        
        result.setSuccessCount(result.getSuccessCount() + 1);
        log.info("成功导入题目: {}", problem.getTitle());
    }
    
    /**
     * 检查题目是否已存在
     * 
     * @param title 题目标题
     * @return 是否重复
     */
    private boolean isDuplicate(String title) {
        return problemService.count(
            Wrappers.lambdaQuery(AcProblem.class)
                .eq(AcProblem::getTitle, title)
        ) > 0;
    }
    
    /**
     * 将 LeetCode 题目 DTO 映射为 AcProblem 实体
     * 
     * @param dto LeetCode 题目 DTO
     * @return AcProblem 实体
     */
    private AcProblem mapToProblem(LeetCodeProblemDTO dto) {
        AcProblem problem = new AcProblem();
        problem.setTitle(dto.getTitle());
        problem.setDescription(dto.getDescription());
        problem.setCodeSnippets(dto.getCodeSnippets());
        problem.setMetaData(dto.getMetaData());
        problem.setDifficulty(difficultyMapper.mapDifficulty(dto.getDifficulty()));
        problem.setTimeLimit(1000);  // 默认 1000ms
        problem.setMemoryLimit(256); // 默认 256MB
        problem.setSubmitCount(0);
        problem.setAcCount(0);
        problem.setStatus(0);        // 正常状态
        return problem;
    }
    
    /**
     * 处理分类标签
     * 
     * @param problemId 题目 ID
     * @param tags 标签列表
     */
    private void processTags(Integer problemId, List<String> tags) {
        for (String tagName : tags) {
            // 查找或创建分类
            AcProblemCategory category = categoryService.getOne(
                Wrappers.lambdaQuery(AcProblemCategory.class)
                    .eq(AcProblemCategory::getName, tagName)
            );
            
            if (category == null) {
                category = new AcProblemCategory();
                category.setName(tagName);
                categoryService.save(category);
                log.debug("创建新分类: {}", tagName);
            }
            
            // 建立关联
            AcProblemCategoryMap map = new AcProblemCategoryMap();
            map.setProblemId(problemId);
            map.setCategoryId(category.getId());
            categoryMapService.save(map);
        }
    }
    
    /**
     * 保存示例测试用例
     * 
     * @param problemId 题目 ID
     * @param testCases 测试用例列表
     */
    private void saveTestCases(Integer problemId, List<TestCaseDTO> testCases) {
        int sort = 1;
        for (TestCaseDTO dto : testCases) {
            AcTestCase testCase = new AcTestCase();
            testCase.setProblemId(problemId);
            testCase.setInput(dto.getInput());
            testCase.setExpectedOutput(dto.getExpectedOutput());
            testCase.setIsSample(1);  // 公开样例
            testCase.setSort(sort++);
            testCase.setStatus(0);
            testCaseService.save(testCase);
        }
        log.debug("保存 {} 个示例测试用例", testCases.size());
    }
}

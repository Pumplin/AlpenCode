package org.ruoyi.system.crawler.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.crawler.client.LeetCodeApiClient;
import org.ruoyi.system.crawler.domain.dto.LeetCodeProblemDTO;
import org.ruoyi.system.crawler.domain.dto.TestCaseDTO;
import org.ruoyi.system.crawler.domain.vo.CrawlerResultVO;
import org.ruoyi.system.crawler.mapper.DifficultyMapper;
import org.ruoyi.system.crawler.parser.TestCaseParser;
import org.ruoyi.system.crawler.service.impl.AcCrawlerServiceImpl;
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.AcProblemCategory;
import org.ruoyi.system.service.IAcProblemCategoryMapService;
import org.ruoyi.system.service.IAcProblemCategoryService;
import org.ruoyi.system.service.IAcProblemService;
import org.ruoyi.system.service.IAcTestCaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 爬虫服务单元测试
 * 
 * 验证需求：1.4, 2.3, 3.5, 4.1, 4.2
 * 
 * @author AlpenCode
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("爬虫服务单元测试")
class AcCrawlerServiceTest {
    
    @Mock
    private LeetCodeApiClient apiClient;
    
    @Mock
    private TestCaseParser testCaseParser;
    
    @Mock
    private DifficultyMapper difficultyMapper;
    
    @Mock
    private IAcProblemService problemService;
    
    @Mock
    private IAcProblemCategoryService categoryService;
    
    @Mock
    private IAcProblemCategoryMapService categoryMapService;
    
    @Mock
    private IAcTestCaseService testCaseService;
    
    @InjectMocks
    private AcCrawlerServiceImpl crawlerService;
    
    @BeforeEach
    void setUp() {
        // 默认行为：题目不重复
        when(problemService.count(any())).thenReturn(0L);
    }
    
    @Test
    @DisplayName("测试空题目列表处理 - 验证需求 1.4")
    void testEmptyProblemList() {
        // Given: API 返回空列表
        when(apiClient.fetchProblemList(20))
            .thenReturn(Collections.emptyList());
        
        // When: 执行爬取
        CrawlerResultVO result = crawlerService.executeCrawl(20);
        
        // Then: 所有计数器应为 0
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount(), "成功数应为 0");
        assertEquals(0, result.getSkipCount(), "跳过数应为 0");
        assertEquals(0, result.getFailCount(), "失败数应为 0");
        assertTrue(result.getFailedProblems().isEmpty(), "失败列表应为空");
        
        // 验证没有调用详情接口
        verify(apiClient, never()).fetchProblemDetail(anyString());
    }
    
    @Test
    @DisplayName("测试重复题目跳过 - 验证需求 4.1, 4.2")
    void testDuplicateProblemSkip() throws InterruptedException {
        // Given: 数据库中已存在"两数之和"
        LeetCodeProblemDTO duplicate = createProblem("两数之和", "two-sum", "Easy");
        
        when(apiClient.fetchProblemList(1))
            .thenReturn(List.of(duplicate));
        when(problemService.count(argThat(wrapper -> 
            wrapper.getEntity() != null && 
            "两数之和".equals(((AcProblem)wrapper.getEntity()).getTitle())
        ))).thenReturn(1L); // 题目已存在
        
        // When: 执行爬取
        CrawlerResultVO result = crawlerService.executeCrawl(1);
        
        // Then: 应该跳过该题目
        assertEquals(0, result.getSuccessCount(), "成功数应为 0");
        assertEquals(1, result.getSkipCount(), "跳过数应为 1");
        assertEquals(0, result.getFailCount(), "失败数应为 0");
        
        // 验证没有调用详情接口和保存操作
        verify(apiClient, never()).fetchProblemDetail(anyString());
        verify(problemService, never()).save(any());
    }
    
    @Test
    @DisplayName("测试单题失败不影响其他题目 - 验证需求 2.3")
    void testSingleProblemFailureDoesNotAffectOthers() throws InterruptedException {
        // Given: 两道题目，第一道获取详情失败，第二道成功
        LeetCodeProblemDTO problem1 = createProblem("题目1", "problem-1", "Easy");
        LeetCodeProblemDTO problem2 = createProblem("题目2", "problem-2", "Medium");
        LeetCodeProblemDTO detail2 = createProblemWithDescription("题目2", "problem-2", "Medium", "描述2");
        
        when(apiClient.fetchProblemList(2))
            .thenReturn(List.of(problem1, problem2));
        when(apiClient.fetchProblemDetail("problem-1"))
            .thenThrow(new ServiceException("网络超时"));
        when(apiClient.fetchProblemDetail("problem-2"))
            .thenReturn(detail2);
        when(difficultyMapper.mapDifficulty("Medium")).thenReturn(2);
        when(testCaseParser.parseExamples(anyString())).thenReturn(Collections.emptyList());
        when(problemService.save(any())).thenReturn(true);
        
        // When: 执行爬取
        CrawlerResultVO result = crawlerService.executeCrawl(2);
        
        // Then: 第一道失败，第二道成功
        assertEquals(1, result.getSuccessCount(), "成功数应为 1");
        assertEquals(0, result.getSkipCount(), "跳过数应为 0");
        assertEquals(1, result.getFailCount(), "失败数应为 1");
        assertEquals(1, result.getFailedProblems().size(), "失败列表应有 1 条");
        assertEquals("题目1", result.getFailedProblems().get(0).getTitle());
        assertTrue(result.getFailedProblems().get(0).getReason().contains("网络超时"));
        
        // 验证第二道题目被保存
        verify(problemService, times(1)).save(argThat(problem -> 
            "题目2".equals(problem.getTitle())
        ));
    }
    
    @Test
    @DisplayName("测试事务回滚场景 - 验证需求 3.5")
    void testTransactionRollback() throws InterruptedException {
        // Given: 题目保存成功，但分类保存失败
        LeetCodeProblemDTO problem = createProblem("测试题目", "test-problem", "Hard");
        LeetCodeProblemDTO detail = createProblemWithDescriptionAndTags(
            "测试题目", "test-problem", "Hard", "描述", List.of("数组", "动态规划")
        );
        
        when(apiClient.fetchProblemList(1)).thenReturn(List.of(problem));
        when(apiClient.fetchProblemDetail("test-problem")).thenReturn(detail);
        when(difficultyMapper.mapDifficulty("Hard")).thenReturn(3);
        when(testCaseParser.parseExamples(anyString())).thenReturn(Collections.emptyList());
        when(problemService.save(any())).thenReturn(true);
        
        // 模拟分类服务抛出异常
        when(categoryService.getOne(any()))
            .thenThrow(new RuntimeException("数据库连接失败"));
        
        // When: 执行爬取
        CrawlerResultVO result = crawlerService.executeCrawl(1);
        
        // Then: 应该记录为失败
        assertEquals(0, result.getSuccessCount(), "成功数应为 0");
        assertEquals(1, result.getFailCount(), "失败数应为 1");
        assertEquals("测试题目", result.getFailedProblems().get(0).getTitle());
        
        // 注意：由于使用了 @Transactional，实际的事务回滚由 Spring 处理
        // 在单元测试中，我们验证异常被正确捕获和记录
    }
    
    @Test
    @DisplayName("测试成功爬取完整流程")
    void testSuccessfulCrawlWithTestCases() throws InterruptedException {
        // Given: 完整的题目数据，包含测试用例
        LeetCodeProblemDTO problem = createProblem("两数之和", "two-sum", "Easy");
        LeetCodeProblemDTO detail = createProblemWithDescriptionAndTags(
            "两数之和", "two-sum", "Easy", 
            "<p>示例 1:</p><p>输入：nums = [2,7,11,15], target = 9</p><p>输出：[0,1]</p>",
            List.of("数组", "哈希表")
        );
        
        TestCaseDTO testCase = new TestCaseDTO();
        testCase.setInput("nums = [2,7,11,15], target = 9");
        testCase.setExpectedOutput("[0,1]");
        
        AcProblemCategory category1 = new AcProblemCategory();
        category1.setId(1);
        category1.setName("数组");
        
        AcProblemCategory category2 = new AcProblemCategory();
        category2.setId(2);
        category2.setName("哈希表");
        
        when(apiClient.fetchProblemList(1)).thenReturn(List.of(problem));
        when(apiClient.fetchProblemDetail("two-sum")).thenReturn(detail);
        when(difficultyMapper.mapDifficulty("Easy")).thenReturn(1);
        when(testCaseParser.parseExamples(anyString())).thenReturn(List.of(testCase));
        when(problemService.save(any())).thenReturn(true);
        when(categoryService.getOne(argThat(wrapper -> {
            AcProblemCategory entity = (AcProblemCategory) wrapper.getEntity();
            return entity != null && "数组".equals(entity.getName());
        }))).thenReturn(category1);
        when(categoryService.getOne(argThat(wrapper -> {
            AcProblemCategory entity = (AcProblemCategory) wrapper.getEntity();
            return entity != null && "哈希表".equals(entity.getName());
        }))).thenReturn(category2);
        when(categoryMapService.save(any())).thenReturn(true);
        when(testCaseService.save(any())).thenReturn(true);
        
        // When: 执行爬取
        CrawlerResultVO result = crawlerService.executeCrawl(1);
        
        // Then: 应该成功
        assertEquals(1, result.getSuccessCount(), "成功数应为 1");
        assertEquals(0, result.getSkipCount(), "跳过数应为 0");
        assertEquals(0, result.getFailCount(), "失败数应为 0");
        
        // 验证所有保存操作都被调用
        verify(problemService, times(1)).save(any());
        verify(categoryMapService, times(2)).save(any()); // 两个分类
        verify(testCaseService, times(1)).save(any()); // 一个测试用例
    }
    
    @Test
    @DisplayName("测试 API 异常处理")
    void testApiException() {
        // Given: API 调用失败
        when(apiClient.fetchProblemList(20))
            .thenThrow(new ServiceException("无法连接到 LeetCode API"));
        
        // When & Then: 应该抛出 ServiceException
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            crawlerService.executeCrawl(20);
        });
        
        assertTrue(exception.getMessage().contains("爬取失败"));
    }
    
    @Test
    @DisplayName("测试多题目混合场景")
    void testMixedScenario() throws InterruptedException {
        // Given: 5 道题目 - 2 成功，1 跳过（重复），2 失败
        List<LeetCodeProblemDTO> problems = List.of(
            createProblem("题目1", "problem-1", "Easy"),
            createProblem("题目2", "problem-2", "Medium"),
            createProblem("题目3", "problem-3", "Hard"),
            createProblem("题目4", "problem-4", "Easy"),
            createProblem("题目5", "problem-5", "Medium")
        );
        
        when(apiClient.fetchProblemList(5)).thenReturn(problems);
        
        // 题目1: 成功
        when(apiClient.fetchProblemDetail("problem-1"))
            .thenReturn(createProblemWithDescription("题目1", "problem-1", "Easy", "描述1"));
        when(difficultyMapper.mapDifficulty("Easy")).thenReturn(1);
        
        // 题目2: 跳过（重复）
        when(problemService.count(argThat(wrapper -> {
            AcProblem entity = (AcProblem) wrapper.getEntity();
            return entity != null && "题目2".equals(entity.getTitle());
        }))).thenReturn(1L);
        
        // 题目3: 失败（网络错误）
        when(apiClient.fetchProblemDetail("problem-3"))
            .thenThrow(new ServiceException("网络超时"));
        
        // 题目4: 成功
        when(apiClient.fetchProblemDetail("problem-4"))
            .thenReturn(createProblemWithDescription("题目4", "problem-4", "Easy", "描述4"));
        
        // 题目5: 失败（难度映射错误）
        when(apiClient.fetchProblemDetail("problem-5"))
            .thenReturn(createProblemWithDescription("题目5", "problem-5", "Unknown", "描述5"));
        when(difficultyMapper.mapDifficulty("Unknown"))
            .thenThrow(new IllegalArgumentException("未知的难度值"));
        
        when(testCaseParser.parseExamples(anyString())).thenReturn(Collections.emptyList());
        when(problemService.save(any())).thenReturn(true);
        
        // When: 执行爬取
        CrawlerResultVO result = crawlerService.executeCrawl(5);
        
        // Then: 验证统计数据
        assertEquals(2, result.getSuccessCount(), "成功数应为 2");
        assertEquals(1, result.getSkipCount(), "跳过数应为 1");
        assertEquals(2, result.getFailCount(), "失败数应为 2");
        assertEquals(2, result.getFailedProblems().size(), "失败列表应有 2 条");
        
        // 验证失败题目
        List<String> failedTitles = result.getFailedProblems().stream()
            .map(f -> f.getTitle())
            .toList();
        assertTrue(failedTitles.contains("题目3"));
        assertTrue(failedTitles.contains("题目5"));
    }
    
    // ========== 辅助方法 ==========
    
    private LeetCodeProblemDTO createProblem(String title, String titleSlug, String difficulty) {
        LeetCodeProblemDTO dto = new LeetCodeProblemDTO();
        dto.setTitle(title);
        dto.setTitleSlug(titleSlug);
        dto.setDifficulty(difficulty);
        dto.setTags(new ArrayList<>());
        return dto;
    }
    
    private LeetCodeProblemDTO createProblemWithDescription(String title, String titleSlug, 
                                                            String difficulty, String description) {
        LeetCodeProblemDTO dto = createProblem(title, titleSlug, difficulty);
        dto.setDescription(description);
        return dto;
    }
    
    private LeetCodeProblemDTO createProblemWithDescriptionAndTags(String title, String titleSlug,
                                                                   String difficulty, String description,
                                                                   List<String> tags) {
        LeetCodeProblemDTO dto = createProblemWithDescription(title, titleSlug, difficulty, description);
        dto.setTags(tags);
        return dto;
    }
}

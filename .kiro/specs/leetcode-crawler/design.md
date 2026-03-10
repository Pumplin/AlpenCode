# 技术设计文档：LeetCode 题目爬虫

## 概述

本文档描述 LeetCode 题目爬虫功能的技术设计方案。该功能通过调用 LeetCode 的公开 GraphQL API 获取题目数据，并将数据映射后导入到 AlpenCode 平台的题库中。功能包括题目列表获取、题目详情获取、示例测试用例解析、数据存储、去重机制，以及管理端的可视化操作界面。

### 设计目标

- 提供稳定可靠的 LeetCode 数据爬取能力
- 实现完整的数据映射和存储流程
- 支持去重机制避免重复导入
- 提供友好的管理端操作界面
- 确保数据一致性和错误处理

### 技术栈

- 后端：Java 17 + Spring Boot 3.4.4 + MyBatis-Plus
- HTTP 客户端：Spring WebClient（支持异步和响应式编程）
- 数据库：MySQL 8.0
- 前端：Vue 3 + TypeScript + Ant Design Vue + Vben Admin

## 系统架构

### 组件划分


```
┌─────────────────────────────────────────────────────────────┐
│                      管理端前端                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  CrawlerPage.vue (爬虫管理页面)                       │  │
│  │  - 输入框（题目数量）                                  │  │
│  │  - 开始爬取按钮                                        │  │
│  │  - 进度显示                                            │  │
│  │  - 结果展示                                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │ HTTP POST
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      后端服务层                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  AcCrawlerController (控制器)                         │  │
│  │  - POST /oj/crawler/execute                           │  │
│  │  - 权限控制：@SaCheckPermission("oj:crawler:execute") │  │
│  └──────────────────────────────────────────────────────┘  │
│                            │
│                            ▼
│  ┌──────────────────────────────────────────────────────┐  │
│  │  IAcCrawlerService (爬虫服务接口)                     │  │
│  │  AcCrawlerServiceImpl (爬虫服务实现)                  │  │
│  │  - executeCrawl() 主流程                              │  │
│  │  - fetchProblemList() 获取题目列表                    │  │
│  │  - fetchProblemDetail() 获取题目详情                  │  │
│  │  - saveProblem() 保存题目                             │  │
│  └──────────────────────────────────────────────────────┘  │
│                            │
│         ┌──────────────────┼──────────────────┐            │
│         ▼                  ▼                  ▼            │
│  ┌─────────────┐  ┌─────────────────┐  ┌──────────────┐  │
│  │ LeetCode    │  │ TestCaseParser  │  │ Difficulty   │  │
│  │ ApiClient   │  │ (示例解析器)     │  │ Mapper       │  │
│  │ (API客户端) │  │                 │  │ (难度映射)    │  │
│  └─────────────┘  └─────────────────┘  └──────────────┘  │
│         │                                                   │
│         ▼                                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  LeetCode GraphQL API                                 │  │
│  │  https://leetcode.cn/graphql/                         │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据持久层                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  IAcProblemService (题目服务)                         │  │
│  │  IAcProblemCategoryService (分类服务)                 │  │
│  │  IAcTestCaseService (测试用例服务)                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                            │
│                            ▼
│  ┌──────────────────────────────────────────────────────┐  │
│  │  MySQL 数据库                                         │  │
│  │  - ac_problem (题目表)                                │  │
│  │  - ac_problem_category (分类表)                       │  │
│  │  - ac_problem_category_map (题目-分类映射表)          │  │
│  │  - ac_test_case (测试用例表)                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件说明


1. **AcCrawlerController**：爬虫控制器，提供 HTTP 接口，处理管理员的爬取请求
2. **IAcCrawlerService**：爬虫服务接口，定义爬取的核心业务逻辑
3. **LeetCodeApiClient**：LeetCode API 客户端，封装 GraphQL 请求逻辑
4. **TestCaseParser**：示例测试用例解析器，从 HTML 描述中提取示例数据
5. **DifficultyMapper**：难度映射器，将 LeetCode 难度转换为 AlpenCode 难度值
6. **CrawlerPage.vue**：管理端爬虫页面，提供可视化操作界面

### 包结构设计

所有爬虫相关代码统一放在 `org.ruoyi.system.crawler` 包下：

```
org.ruoyi.system.crawler/
├── domain/
│   ├── dto/                    # 数据传输对象
│   │   ├── LeetCodeProblemDTO.java
│   │   └── TestCaseDTO.java
│   ├── vo/                     # 视图对象
│   │   ├── CrawlerResultVO.java
│   │   └── FailedProblemVO.java
│   └── bo/                     # 业务对象
│       └── CrawlerRequestBO.java
├── service/                    # 服务层
│   ├── IAcCrawlerService.java
│   └── impl/
│       └── AcCrawlerServiceImpl.java
├── controller/                 # 控制器层
│   └── AcCrawlerController.java
├── client/                     # API 客户端
│   └── LeetCodeApiClient.java
├── parser/                     # 解析器
│   └── TestCaseParser.java
├── mapper/                     # 映射器
│   └── DifficultyMapper.java
└── config/                     # 配置类
    └── WebClientConfig.java
```

## 后端 API 设计

### 接口定义

#### 1. 执行爬取操作

```
POST /oj/crawler/execute
```

**权限要求**：`oj:crawler:execute`

**请求参数**：

```json
{
  "limit": 20  // 爬取题目数量，默认 20
}
```

**响应格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "successCount": 15,      // 成功导入数量
    "skipCount": 3,          // 跳过数量（重复）
    "failCount": 2,          // 失败数量
    "failedProblems": [      // 失败题目列表
      {
        "title": "两数之和",
        "reason": "网络超时"
      }
    ]
  }
}
```

**错误响应**：

```json
{
  "code": 500,
  "msg": "爬取失败：无法连接到 LeetCode API",
  "data": null
}
```

## 数据模型设计

### 实体类（Entity）

#### 1. AcProblem（已存在）

```java
@Data
@TableName("ac_problem")
public class AcProblem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    private Integer difficulty;
    private Integer timeLimit;
    private Integer memoryLimit;
    private Integer submitCount;
    private Integer acCount;
    @TableLogic
    private Integer isDelete;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Integer status;
}
```


#### 2. AcTestCase（已存在）

```java
@Data
@TableName("ac_test_case")
public class AcTestCase implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer problemId;
    private String input;
    private String expectedOutput;
    private Integer isSample;  // 1=公开样例，0=隐藏用例
    private Integer sort;
    @TableLogic
    private Integer isDelete;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Integer status;
}
```

### VO（View Object）

#### CrawlerResultVO

```java
@Data
public class CrawlerResultVO {
    /**
     * 成功导入数量
     */
    private Integer successCount;
    
    /**
     * 跳过数量（重复）
     */
    private Integer skipCount;
    
    /**
     * 失败数量
     */
    private Integer failCount;
    
    /**
     * 失败题目列表
     */
    private List<FailedProblemVO> failedProblems;
}

@Data
public class FailedProblemVO {
    /**
     * 题目标题
     */
    private String title;
    
    /**
     * 失败原因
     */
    private String reason;
}
```

### BO（Business Object）

#### CrawlerRequestBO

```java
@Data
public class CrawlerRequestBO {
    /**
     * 爬取题目数量，默认 20
     */
    private Integer limit = 20;
}
```

### DTO（Data Transfer Object）

#### LeetCodeProblemDTO

```java
@Data
public class LeetCodeProblemDTO {
    /**
     * 题目唯一标识（titleSlug）
     */
    private String titleSlug;
    
    /**
     * 题目标题
     */
    private String title;
    
    /**
     * 题目描述（HTML 格式）
     */
    private String description;
    
    /**
     * 难度（Easy/Medium/Hard）
     */
    private String difficulty;
    
    /**
     * 分类标签列表
     */
    private List<String> tags;
    
    /**
     * 示例测试用例列表
     */
    private List<TestCaseDTO> exampleTestCases;
}

@Data
public class TestCaseDTO {
    /**
     * 输入
     */
    private String input;
    
    /**
     * 期望输出
     */
    private String expectedOutput;
}
```


## LeetCode API 调用设计

### GraphQL 查询定义

#### 1. 查询题目列表

```graphql
query problemsetQuestionList($categorySlug: String, $limit: Int, $skip: Int, $filters: QuestionListFilterInput) {
  problemsetQuestionList: questionList(
    categorySlug: $categorySlug
    limit: $limit
    skip: $skip
    filters: $filters
  ) {
    total: totalNum
    questions: data {
      questionId
      questionFrontendId
      title
      titleSlug
      difficulty
      topicTags {
        name
        slug
      }
    }
  }
}
```

**变量**：

```json
{
  "categorySlug": "",
  "limit": 20,
  "skip": 0,
  "filters": {}
}
```

#### 2. 查询题目详情

```graphql
query questionData($titleSlug: String!) {
  question(titleSlug: $titleSlug) {
    questionId
    questionFrontendId
    title
    titleSlug
    content
    translatedTitle
    translatedContent
    difficulty
    topicTags {
      name
      slug
      translatedName
    }
    exampleTestcases
  }
}
```

**变量**：

```json
{
  "titleSlug": "two-sum"
}
```

### LeetCodeApiClient 实现

```java
@Component
public class LeetCodeApiClient {
    
    private static final String LEETCODE_API_URL = "https://leetcode.cn/graphql/";
    private static final int REQUEST_DELAY_MS = 500;
    
    private final WebClient webClient;
    
    public LeetCodeApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl(LEETCODE_API_URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .build();
    }
    
    /**
     * 获取题目列表
     */
    public List<LeetCodeProblemDTO> fetchProblemList(int limit) {
        String query = buildProblemListQuery();
        Map<String, Object> variables = Map.of(
            "categorySlug", "",
            "limit", limit,
            "skip", 0,
            "filters", Map.of()
        );
        
        // 发送 GraphQL 请求
        // 解析响应并返回题目列表
    }
    
    /**
     * 获取题目详情
     */
    public LeetCodeProblemDTO fetchProblemDetail(String titleSlug) 
            throws InterruptedException {
        // 请求频率控制
        Thread.sleep(REQUEST_DELAY_MS);
        
        String query = buildProblemDetailQuery();
        Map<String, Object> variables = Map.of("titleSlug", titleSlug);
        
        // 发送 GraphQL 请求
        // 解析响应并返回题目详情
    }
}
```


### DifficultyMapper 实现

```java
@Component
public class DifficultyMapper {
    
    /**
     * 将 LeetCode 难度映射为 AlpenCode 难度值
     * Easy -> 1
     * Medium -> 2
     * Hard -> 3
     */
    public Integer mapDifficulty(String leetcodeDifficulty) {
        return switch (leetcodeDifficulty.toLowerCase()) {
            case "easy" -> 1;
            case "medium" -> 2;
            case "hard" -> 3;
            default -> throw new IllegalArgumentException(
                "未知的难度值: " + leetcodeDifficulty);
        };
    }
}
```

### TestCaseParser 实现

```java
@Component
public class TestCaseParser {
    
    /**
     * 从 HTML 描述中解析示例测试用例
     * 
     * 解析规则：
     * 1. 查找 "示例 1"、"示例 2" 等标题
     * 2. 提取 "输入：" 后的内容作为 input
     * 3. 提取 "输出：" 后的内容作为 expectedOutput
     * 4. 支持多组示例
     */
    public List<TestCaseDTO> parseExamples(String htmlContent) {
        List<TestCaseDTO> testCases = new ArrayList<>();
        
        // 使用 Jsoup 解析 HTML
        Document doc = Jsoup.parse(htmlContent);
        
        // 查找所有示例块
        Elements examples = doc.select("p:contains(示例)");
        
        for (Element example : examples) {
            try {
                TestCaseDTO testCase = extractTestCase(example);
                if (testCase != null) {
                    testCases.add(testCase);
                }
            } catch (Exception e) {
                log.warn("解析示例失败: {}", e.getMessage());
            }
        }
        
        return testCases;
    }
    
    private TestCaseDTO extractTestCase(Element exampleElement) {
        // 查找 "输入：" 和 "输出：" 的内容
        String input = extractContent(exampleElement, "输入");
        String output = extractContent(exampleElement, "输出");
        
        if (input != null && output != null) {
            TestCaseDTO testCase = new TestCaseDTO();
            testCase.setInput(input.trim());
            testCase.setExpectedOutput(output.trim());
            return testCase;
        }
        
        return null;
    }
    
    private String extractContent(Element element, String label) {
        // 实现内容提取逻辑
        // 查找包含 label 的元素，提取其后的文本内容
        return null;
    }
}
```

## 数据映射与存储流程

### 核心流程图

```
开始爬取
    │
    ▼
获取题目列表 (LeetCodeApiClient.fetchProblemList)
    │
    ▼
遍历题目列表
    │
    ├─► 检查标题是否重复 (根据 title 查询 ac_problem)
    │   │
    │   ├─► 已存在 → 跳过，skipCount++
    │   │
    │   └─► 不存在 → 继续
    │
    ├─► 获取题目详情 (LeetCodeApiClient.fetchProblemDetail)
    │   │
    │   ├─► 成功 → 继续
    │   │
    │   └─► 失败 → 记录失败信息，failCount++，继续下一题
    │
    ├─► 解析示例测试用例 (TestCaseParser.parseExamples)
    │
    ├─► 映射难度值 (DifficultyMapper.mapDifficulty)
    │
    ├─► 开启数据库事务
    │   │
    │   ├─► 保存题目 (ac_problem)
    │   │
    │   ├─► 处理分类标签
    │   │   │
    │   │   ├─► 检查分类是否存在
    │   │   │   │
    │   │   │   ├─► 存在 → 获取分类 ID
    │   │   │   │
    │   │   │   └─► 不存在 → 创建新分类
    │   │   │
    │   │   └─► 建立题目-分类关联 (ac_problem_category_map)
    │   │
    │   ├─► 保存示例测试用例 (ac_test_case, is_sample=1)
    │   │
    │   ├─► 提交事务
    │   │   │
    │   │   ├─► 成功 → successCount++
    │   │   │
    │   │   └─► 失败 → 回滚，failCount++
    │   │
    │   └─► 请求延迟 (500ms)
    │
    └─► 下一题
    
返回执行结果 (CrawlerResultVO)
```


### AcCrawlerService 实现

```java
public interface IAcCrawlerService {
    /**
     * 执行爬取操作
     * @param limit 爬取题目数量
     * @return 爬取结果
     */
    CrawlerResultVO executeCrawl(Integer limit);
}

@Service
@RequiredArgsConstructor
@Slf4j
public class AcCrawlerServiceImpl implements IAcCrawlerService {
    
    private final LeetCodeApiClient apiClient;
    private final TestCaseParser testCaseParser;
    private final DifficultyMapper difficultyMapper;
    private final IAcProblemService problemService;
    private final IAcProblemCategoryService categoryService;
    private final IAcTestCaseService testCaseService;
    
    @Override
    public CrawlerResultVO executeCrawl(Integer limit) {
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
            
            // 2. 遍历处理每道题目
            for (LeetCodeProblemDTO leetcodeProblem : problemList) {
                try {
                    processSingleProblem(leetcodeProblem, result);
                } catch (Exception e) {
                    log.error("处理题目失败: {}", leetcodeProblem.getTitle(), e);
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailedProblems().add(new FailedProblemVO(
                        leetcodeProblem.getTitle(), 
                        e.getMessage()
                    ));
                }
            }
            
        } catch (Exception e) {
            log.error("爬取操作失败", e);
            throw new ServiceException("爬取失败: " + e.getMessage());
        }
        
        return result;
    }
    
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
    
    private boolean isDuplicate(String title) {
        return problemService.count(
            Wrappers.lambdaQuery(AcProblem.class)
                .eq(AcProblem::getTitle, title)
        ) > 0;
    }
    
    private AcProblem mapToProblem(LeetCodeProblemDTO dto) {
        AcProblem problem = new AcProblem();
        problem.setTitle(dto.getTitle());
        problem.setDescription(dto.getDescription());
        problem.setDifficulty(difficultyMapper.mapDifficulty(dto.getDifficulty()));
        problem.setTimeLimit(1000);  // 默认 1000ms
        problem.setMemoryLimit(256); // 默认 256MB
        problem.setSubmitCount(0);
        problem.setAcCount(0);
        problem.setStatus(0);        // 正常状态
        return problem;
    }
    
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
            }
            
            // 建立关联
            AcProblemCategoryMap map = new AcProblemCategoryMap();
            map.setProblemId(problemId);
            map.setCategoryId(category.getId());
            categoryMapService.save(map);
        }
    }
    
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
    }
}
```


### AcCrawlerController 实现

```java
@RestController
@RequestMapping("/oj/crawler")
@RequiredArgsConstructor
@Slf4j
public class AcCrawlerController extends BaseController {
    
    private final IAcCrawlerService crawlerService;
    
    /**
     * 执行爬取操作
     * 
     * @param request 爬取请求参数
     * @return 爬取结果
     */
    @SaCheckPermission("oj:crawler:execute")
    @PostMapping("/execute")
    @Log(title = "LeetCode爬虫", businessType = BusinessType.OTHER)
    public R<CrawlerResultVO> execute(@RequestBody CrawlerRequestBO request) {
        Integer limit = request.getLimit();
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (limit > 100) {
            return R.fail("单次爬取数量不能超过 100");
        }
        
        try {
            CrawlerResultVO result = crawlerService.executeCrawl(limit);
            return R.ok(result);
        } catch (Exception e) {
            log.error("爬取操作失败", e);
            return R.fail("爬取失败: " + e.getMessage());
        }
    }
}
```

## 前端页面设计

### 组件结构

```
views/oj/crawler/
├── index.vue              # 爬虫管理页面主文件
└── components/
    ├── CrawlerForm.vue    # 爬取表单组件
    └── ResultDisplay.vue  # 结果展示组件
```

### CrawlerPage 实现（index.vue）

```vue
<template>
  <div class="crawler-page">
    <a-card title="LeetCode 题目爬虫" :bordered="false">
      <!-- 爬取表单 -->
      <a-form layout="inline" :model="formState">
        <a-form-item label="爬取数量">
          <a-input-number
            v-model:value="formState.limit"
            :min="1"
            :max="100"
            :disabled="crawling"
            placeholder="请输入爬取数量"
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            :loading="crawling"
            @click="handleCrawl"
          >
            {{ crawling ? '爬取中...' : '开始爬取' }}
          </a-button>
        </a-form-item>
      </a-form>

      <!-- 进度显示 -->
      <div v-if="crawling" class="progress-section">
        <a-spin tip="正在爬取题目数据，请稍候...">
          <a-alert
            message="爬取进行中"
            description="正在从 LeetCode 获取题目数据并导入到题库"
            type="info"
          />
        </a-spin>
      </div>

      <!-- 结果展示 -->
      <div v-if="result" class="result-section">
        <a-divider>爬取结果</a-divider>
        
        <a-row :gutter="16">
          <a-col :span="8">
            <a-statistic
              title="成功导入"
              :value="result.successCount"
              :value-style="{ color: '#3f8600' }"
            >
              <template #prefix>
                <CheckCircleOutlined />
              </template>
            </a-statistic>
          </a-col>
          <a-col :span="8">
            <a-statistic
              title="跳过（重复）"
              :value="result.skipCount"
              :value-style="{ color: '#faad14' }"
            >
              <template #prefix>
                <MinusCircleOutlined />
              </template>
            </a-statistic>
          </a-col>
          <a-col :span="8">
            <a-statistic
              title="失败"
              :value="result.failCount"
              :value-style="{ color: '#cf1322' }"
            >
              <template #prefix>
                <CloseCircleOutlined />
              </template>
            </a-statistic>
          </a-col>
        </a-row>

        <!-- 失败题目列表 -->
        <div v-if="result.failedProblems && result.failedProblems.length > 0">
          <a-divider>失败题目详情</a-divider>
          <a-table
            :columns="failedColumns"
            :data-source="result.failedProblems"
            :pagination="false"
            size="small"
          />
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { message } from 'ant-design-vue';
import { executeCrawler } from '@/api/oj/crawler';
import type { CrawlerResultVO } from '@/api/oj/crawler/model';
import {
  CheckCircleOutlined,
  MinusCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue';

const formState = reactive({
  limit: 20,
});

const crawling = ref(false);
const result = ref<CrawlerResultVO | null>(null);

const failedColumns = [
  {
    title: '题目标题',
    dataIndex: 'title',
    key: 'title',
  },
  {
    title: '失败原因',
    dataIndex: 'reason',
    key: 'reason',
  },
];

const handleCrawl = async () => {
  if (formState.limit < 1 || formState.limit > 100) {
    message.error('爬取数量必须在 1-100 之间');
    return;
  }

  crawling.value = true;
  result.value = null;

  try {
    const res = await executeCrawler({ limit: formState.limit });
    result.value = res;
    
    if (res.successCount > 0) {
      message.success(`成功导入 ${res.successCount} 道题目`);
    } else {
      message.warning('未导入任何题目');
    }
  } catch (error) {
    message.error('爬取失败: ' + error.message);
  } finally {
    crawling.value = false;
  }
};
</script>

<style scoped lang="less">
.crawler-page {
  .progress-section {
    margin-top: 24px;
  }

  .result-section {
    margin-top: 24px;
  }
}
</style>
```


### API 定义（api/oj/crawler/index.ts）

```typescript
import { defHttp } from '@/utils/http/axios';
import type { CrawlerRequestBO, CrawlerResultVO } from './model';

enum Api {
  Execute = '/oj/crawler/execute',
}

/**
 * 执行爬取操作
 */
export function executeCrawler(params: CrawlerRequestBO) {
  return defHttp.post<CrawlerResultVO>({
    url: Api.Execute,
    data: params,
  });
}
```

### 类型定义（api/oj/crawler/model.d.ts）

```typescript
/**
 * 爬取请求参数
 */
export interface CrawlerRequestBO {
  /**
   * 爬取题目数量
   */
  limit: number;
}

/**
 * 爬取结果
 */
export interface CrawlerResultVO {
  /**
   * 成功导入数量
   */
  successCount: number;
  
  /**
   * 跳过数量（重复）
   */
  skipCount: number;
  
  /**
   * 失败数量
   */
  failCount: number;
  
  /**
   * 失败题目列表
   */
  failedProblems: FailedProblemVO[];
}

/**
 * 失败题目信息
 */
export interface FailedProblemVO {
  /**
   * 题目标题
   */
  title: string;
  
  /**
   * 失败原因
   */
  reason: string;
}
```

## 错误处理策略

### 错误分类

1. **网络错误**
   - 连接超时
   - DNS 解析失败
   - 网络不可达

2. **API 错误**
   - LeetCode API 返回错误响应
   - GraphQL 查询语法错误
   - 认证失败（如需要登录）

3. **数据解析错误**
   - JSON 解析失败
   - HTML 解析失败
   - 字段缺失或格式不符

4. **数据库错误**
   - 事务提交失败
   - 唯一约束冲突
   - 外键约束冲突

5. **业务逻辑错误**
   - 难度值映射失败
   - 分类名称过长
   - 题目描述为空

### 错误处理策略


### 包路径说明

- Controller: `org.ruoyi.system.crawler.controller.AcCrawlerController`
- Service: `org.ruoyi.system.crawler.service.IAcCrawlerService` 和 `org.ruoyi.system.crawler.service.impl.AcCrawlerServiceImpl`
- API Client: `org.ruoyi.system.crawler.client.LeetCodeApiClient`
- Parser: `org.ruoyi.system.crawler.parser.TestCaseParser`
- Mapper: `org.ruoyi.system.crawler.mapper.DifficultyMapper`
- DTO: `org.ruoyi.system.crawler.domain.dto.*`
- VO: `org.ruoyi.system.crawler.domain.vo.*`
- BO: `org.ruoyi.system.crawler.domain.bo.*`
- Config: `org.ruoyi.system.crawler.config.WebClientConfig`

#### 1. 网络错误处理

```java
@Retryable(
    value = {WebClientRequestException.class, TimeoutException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 2000)
)
public LeetCodeProblemDTO fetchProblemDetail(String titleSlug) {
    try {
        return webClient.post()
            .bodyValue(buildRequest(query, variables))
            .retrieve()
            .bodyToMono(LeetCodeResponse.class)
            .timeout(Duration.ofSeconds(10))
            .block();
    } catch (WebClientRequestException e) {
        log.error("网络请求失败: {}", titleSlug, e);
        throw new ServiceException("无法连接到 LeetCode API");
    } catch (TimeoutException e) {
        log.error("请求超时: {}", titleSlug, e);
        throw new ServiceException("LeetCode API 响应超时");
    }
}
```

#### 2. API 错误处理

```java
private void validateApiResponse(LeetCodeResponse response) {
    if (response.getErrors() != null && !response.getErrors().isEmpty()) {
        String errorMsg = response.getErrors().stream()
            .map(Error::getMessage)
            .collect(Collectors.joining(", "));
        throw new ServiceException("LeetCode API 错误: " + errorMsg);
    }
    
    if (response.getData() == null) {
        throw new ServiceException("LeetCode API 返回空数据");
    }
}
```

#### 3. 数据解析错误处理

```java
private TestCaseDTO extractTestCase(Element exampleElement) {
    try {
        String input = extractContent(exampleElement, "输入");
        String output = extractContent(exampleElement, "输出");
        
        if (StringUtils.isBlank(input) || StringUtils.isBlank(output)) {
            log.warn("示例数据不完整，跳过");
            return null;
        }
        
        TestCaseDTO testCase = new TestCaseDTO();
        testCase.setInput(input.trim());
        testCase.setExpectedOutput(output.trim());
        return testCase;
        
    } catch (Exception e) {
        log.warn("解析示例失败: {}", e.getMessage());
        return null;
    }
}
```

#### 4. 数据库错误处理

```java
@Transactional(rollbackFor = Exception.class)
private void processSingleProblem(LeetCodeProblemDTO leetcodeProblem, 
                                  CrawlerResultVO result) {
    try {
        // 数据库操作
        saveProblem();
        processTags();
        saveTestCases();
        
    } catch (DuplicateKeyException e) {
        log.warn("题目已存在（数据库约束）: {}", leetcodeProblem.getTitle());
        result.setSkipCount(result.getSkipCount() + 1);
        throw e; // 触发事务回滚
        
    } catch (DataIntegrityViolationException e) {
        log.error("数据完整性错误: {}", leetcodeProblem.getTitle(), e);
        result.setFailCount(result.getFailCount() + 1);
        result.getFailedProblems().add(new FailedProblemVO(
            leetcodeProblem.getTitle(),
            "数据完整性错误"
        ));
        throw e; // 触发事务回滚
    }
}
```

#### 5. 业务逻辑错误处理

```java
public Integer mapDifficulty(String leetcodeDifficulty) {
    try {
        return switch (leetcodeDifficulty.toLowerCase()) {
            case "easy" -> 1;
            case "medium" -> 2;
            case "hard" -> 3;
            default -> {
                log.error("未知的难度值: {}", leetcodeDifficulty);
                throw new IllegalArgumentException(
                    "未知的难度值: " + leetcodeDifficulty);
            }
        };
    } catch (Exception e) {
        log.error("难度映射失败", e);
        throw new ServiceException("难度映射失败: " + e.getMessage());
    }
}
```

### 日志记录策略

```java
@Slf4j
public class AcCrawlerServiceImpl implements IAcCrawlerService {
    
    @Override
    public CrawlerResultVO executeCrawl(Integer limit) {
        log.info("开始爬取 LeetCode 题目，数量: {}", limit);
        
        CrawlerResultVO result = new CrawlerResultVO();
        
        try {
            List<LeetCodeProblemDTO> problemList = apiClient.fetchProblemList(limit);
            log.info("成功获取题目列表，数量: {}", problemList.size());
            
            for (LeetCodeProblemDTO problem : problemList) {
                try {
                    processSingleProblem(problem, result);
                    log.debug("成功处理题目: {}", problem.getTitle());
                } catch (Exception e) {
                    log.error("处理题目失败: {}, 原因: {}", 
                        problem.getTitle(), e.getMessage(), e);
                }
            }
            
            log.info("爬取完成 - 成功: {}, 跳过: {}, 失败: {}",
                result.getSuccessCount(),
                result.getSkipCount(),
                result.getFailCount());
                
        } catch (Exception e) {
            log.error("爬取操作异常", e);
            throw new ServiceException("爬取失败: " + e.getMessage());
        }
        
        return result;
    }
}
```


## 正确性属性

属性是一种特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的正式陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。

### 属性反思

在编写正确性属性之前，我们需要审查预工作分析中识别的所有可测试属性，消除冗余：

**冗余分析**：

1. **API 调用相关**：
   - 属性 1.1（调用 API 获取列表）和属性 2.1（逐个获取详情）可以合并为一个更全面的属性：对于任何题目列表，服务应该为每个题目获取详情
   - 保留：合并后的属性

2. **数据提取相关**：
   - 属性 1.2（提取必需字段）已经涵盖了数据提取的完整性
   - 保留：属性 1.2

3. **错误处理相关**：
   - 属性 1.3（API 错误处理）和属性 2.3（单题失败容错）都是关于错误处理，但测试不同场景
   - 保留：两个属性都保留，因为它们测试不同的错误场景

4. **数据存储相关**：
   - 属性 3.2（存储题目数据）、3.3（分类去重）、3.4（建立关联）可以合并为一个事务完整性属性
   - 属性 3.5（事务完整性）已经涵盖了这些内容
   - 保留：属性 3.5（事务完整性），移除 3.2、3.3、3.4

5. **去重相关**：
   - 属性 4.1（检查重复）和属性 4.2（跳过重复）可以合并为一个属性
   - 保留：合并后的属性

6. **统计相关**：
   - 属性 4.3（执行摘要统计）已经涵盖了结果统计的准确性
   - 保留：属性 4.3

7. **测试用例解析相关**：
   - 属性 7.1（识别示例）、7.2（多示例解析）、7.3（round-trip）可以合并
   - 属性 7.3（round-trip）是最强的属性，涵盖了解析的正确性
   - 保留：属性 7.3（round-trip）

8. **权限控制**：
   - 属性 5.2（权限控制）是独立的安全属性
   - 保留：属性 5.2

**最终保留的属性**：
1. API 调用完整性（合并 1.1 + 2.1）
2. 数据字段提取完整性（1.2）
3. API 错误处理（1.3）
4. 请求频率控制（2.2）
5. 单题失败容错（2.3）
6. 难度映射正确性（3.1）
7. 事务完整性（3.5）
8. 去重机制（合并 4.1 + 4.2）
9. 统计准确性（4.3）
10. 权限控制（5.2）
11. 响应格式完整性（5.3）
12. 示例解析 round-trip（7.3）

### 属性定义

#### 属性 1：API 调用完整性

*对于任何*题目列表，爬虫服务应该为列表中的每个题目调用详情接口，且调用次数等于列表长度（排除重复题目）

**验证需求：1.1, 2.1**

#### 属性 2：数据字段提取完整性

*对于任何*从 LeetCode API 返回的题目数据，提取后的 DTO 应该包含所有必需字段（titleSlug、title、difficulty、tags），且字段值非空

**验证需求：1.2**

#### 属性 3：API 错误恢复

*对于任何*API 错误响应（网络超时、4xx、5xx），爬虫服务应该记录错误日志并返回包含错误原因的失败响应，而不是抛出未捕获异常

**验证需求：1.3**

#### 属性 4：请求频率限制

*对于任何*连续的两次题目详情请求，它们之间的时间间隔应该不小于 500ms

**验证需求：2.2**

#### 属性 5：部分失败容错

*对于任何*题目列表，如果某道题目的详情获取失败，爬虫服务应该继续处理剩余题目，且失败题目被正确记录在失败列表中

**验证需求：2.3**

#### 属性 6：难度映射双射性

*对于任何*有效的 LeetCode 难度值（Easy/Medium/Hard），映射后再反向映射应该得到原始值（双射关系）

**验证需求：3.1**

#### 属性 7：数据存储事务完整性

*对于任何*题目数据，如果存储过程中任何步骤（保存题目、创建分类、建立关联、保存测试用例）失败，则整个事务应该回滚，数据库中不应该存在部分数据

**验证需求：3.2, 3.3, 3.4, 3.5**

#### 属性 8：去重机制有效性

*对于任何*已存在于数据库中的题目标题，再次爬取时应该被跳过，且跳过计数器增加 1，数据库中不应该出现重复标题的题目

**验证需求：4.1, 4.2**

#### 属性 9：统计数据一致性

*对于任何*爬取操作，返回的统计数据（successCount + skipCount + failCount）应该等于处理的题目总数

**验证需求：4.3**

#### 属性 10：权限控制有效性

*对于任何*不具有 `oj:crawler:execute` 权限的用户，调用爬虫接口应该返回 403 权限拒绝错误

**验证需求：5.2**

#### 属性 11：响应格式完整性

*对于任何*爬取操作的响应，JSON 数据应该包含所有必需字段（successCount、skipCount、failCount、failedProblems），且字段类型正确

**验证需求：5.3**

#### 属性 12：示例解析 Round-Trip

*对于任何*成功解析的示例测试用例，将其格式化输出后再解析应该得到语义等价的输入输出数据

**验证需求：7.3**


## 测试策略

### 双重测试方法

本功能采用单元测试和基于属性的测试相结合的方法，以确保全面覆盖：

- **单元测试**：验证特定示例、边界情况和错误条件
- **属性测试**：通过随机输入验证通用属性，确保系统在各种场景下的正确性

两种测试方法是互补的，都是必需的：
- 单元测试捕获具体的 bug 和边界情况
- 属性测试验证跨所有输入的一般正确性

### 单元测试

单元测试应该专注于：

1. **特定示例**
   - 测试已知的 LeetCode 题目数据（如"两数之和"）
   - 验证正确的数据映射和存储
   - 测试典型的成功场景

2. **边界情况**
   - 空题目列表
   - 题目描述为空
   - 无分类标签的题目
   - 无示例测试用例的题目
   - 单个题目和大量题目（100 个）

3. **错误条件**
   - 网络连接失败
   - API 返回错误响应
   - 数据库连接失败
   - 事务回滚场景
   - 无效的难度值

4. **集成点**
   - LeetCodeApiClient 与 WebClient 的集成
   - Service 层与 Mapper 层的集成
   - Controller 与 Service 的集成

**示例单元测试**：

```java
@SpringBootTest
class AcCrawlerServiceTest {
    
    @Autowired
    private IAcCrawlerService crawlerService;
    
    @MockBean
    private LeetCodeApiClient apiClient;
    
    @Test
    @DisplayName("测试空题目列表处理")
    void testEmptyProblemList() {
        // Given
        when(apiClient.fetchProblemList(20))
            .thenReturn(Collections.emptyList());
        
        // When
        CrawlerResultVO result = crawlerService.executeCrawl(20);
        
        // Then
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getSkipCount());
        assertEquals(0, result.getFailCount());
    }
    
    @Test
    @DisplayName("测试重复题目跳过")
    void testDuplicateProblemSkip() {
        // Given: 数据库中已存在"两数之和"
        AcProblem existing = new AcProblem();
        existing.setTitle("两数之和");
        problemService.save(existing);
        
        LeetCodeProblemDTO duplicate = new LeetCodeProblemDTO();
        duplicate.setTitle("两数之和");
        duplicate.setTitleSlug("two-sum");
        
        when(apiClient.fetchProblemList(1))
            .thenReturn(List.of(duplicate));
        
        // When
        CrawlerResultVO result = crawlerService.executeCrawl(1);
        
        // Then
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getSkipCount());
    }
    
    @Test
    @DisplayName("测试单题失败不影响其他题目")
    void testSingleProblemFailure() {
        // Given
        LeetCodeProblemDTO problem1 = createValidProblem("题目1");
        LeetCodeProblemDTO problem2 = createValidProblem("题目2");
        
        when(apiClient.fetchProblemList(2))
            .thenReturn(List.of(problem1, problem2));
        when(apiClient.fetchProblemDetail("problem-1"))
            .thenThrow(new ServiceException("网络超时"));
        when(apiClient.fetchProblemDetail("problem-2"))
            .thenReturn(problem2);
        
        // When
        CrawlerResultVO result = crawlerService.executeCrawl(2);
        
        // Then
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals("题目1", result.getFailedProblems().get(0).getTitle());
    }
}
```

### 基于属性的测试

基于属性的测试使用 **JUnit QuickCheck** 库（Java 的属性测试框架）。

**配置要求**：
- 每个属性测试最少运行 100 次迭代
- 每个测试必须引用设计文档中的属性
- 标签格式：`@Tag("Feature: leetcode-crawler, Property X: [property_text]")`

**依赖配置（pom.xml）**：

```xml
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-core</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-generators</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```

**属性测试示例**：

```java
@RunWith(JUnitQuickcheck.class)
public class CrawlerServicePropertyTest {
    
    @Property(trials = 100)
    @Tag("Feature: leetcode-crawler, Property 6: 难度映射双射性")
    public void difficultyMappingBijection(
            @From(DifficultyGenerator.class) String difficulty) {
        // Given
        DifficultyMapper mapper = new DifficultyMapper();
        
        // When: 映射后再反向映射
        Integer mapped = mapper.mapDifficulty(difficulty);
        String reversed = mapper.reverseDifficulty(mapped);
        
        // Then: 应该得到原始值
        assertEquals(difficulty.toLowerCase(), reversed.toLowerCase());
    }
    
    @Property(trials = 100)
    @Tag("Feature: leetcode-crawler, Property 9: 统计数据一致性")
    public void statisticsConsistency(
            @InRange(min = "1", max = "50") int problemCount) {
        // Given: 模拟爬取结果
        CrawlerResultVO result = simulateCrawl(problemCount);
        
        // Then: 总数应该一致
        int total = result.getSuccessCount() + 
                    result.getSkipCount() + 
                    result.getFailCount();
        assertEquals(problemCount, total);
    }
    
    @Property(trials = 100)
    @Tag("Feature: leetcode-crawler, Property 8: 去重机制有效性")
    public void deduplicationEffectiveness(
            @From(ProblemGenerator.class) LeetCodeProblemDTO problem) {
        // Given: 第一次保存题目
        crawlerService.saveProblem(problem);
        int initialCount = problemService.count();
        
        // When: 再次保存相同标题的题目
        CrawlerResultVO result = crawlerService.executeCrawl(
            List.of(problem)
        );
        
        // Then: 应该被跳过，数据库记录数不变
        assertEquals(1, result.getSkipCount());
        assertEquals(initialCount, problemService.count());
    }
    
    // 自定义生成器
    public static class DifficultyGenerator extends Generator<String> {
        public DifficultyGenerator() {
            super(String.class);
        }
        
        @Override
        public String generate(SourceOfRandomness random, GenerationStatus status) {
            String[] difficulties = {"Easy", "Medium", "Hard"};
            return difficulties[random.nextInt(difficulties.length)];
        }
    }
    
    public static class ProblemGenerator extends Generator<LeetCodeProblemDTO> {
        public ProblemGenerator() {
            super(LeetCodeProblemDTO.class);
        }
        
        @Override
        public LeetCodeProblemDTO generate(
                SourceOfRandomness random, 
                GenerationStatus status) {
            LeetCodeProblemDTO problem = new LeetCodeProblemDTO();
            problem.setTitle("题目" + random.nextInt(1000));
            problem.setTitleSlug("problem-" + random.nextInt(1000));
            problem.setDifficulty(
                new DifficultyGenerator().generate(random, status)
            );
            problem.setDescription("描述内容");
            problem.setTags(generateTags(random));
            return problem;
        }
        
        private List<String> generateTags(SourceOfRandomness random) {
            String[] allTags = {"数组", "字符串", "动态规划", "贪心", "双指针"};
            int count = random.nextInt(1, 4);
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                tags.add(allTags[random.nextInt(allTags.length)]);
            }
            return tags;
        }
    }
}
```

### 测试覆盖率目标

- 代码覆盖率：≥ 80%
- 分支覆盖率：≥ 75%
- 核心业务逻辑覆盖率：≥ 90%

### 测试执行顺序

1. 单元测试（快速反馈）
2. 属性测试（全面验证）
3. 集成测试（端到端验证）


## 部署和配置

### 后端配置

#### application.yml 配置

```yaml
# LeetCode 爬虫配置
leetcode:
  api:
    url: https://leetcode.cn/graphql/
    timeout: 10000  # 请求超时时间（毫秒）
    retry:
      max-attempts: 3  # 最大重试次数
      delay: 2000      # 重试延迟（毫秒）
  crawler:
    request-delay: 500  # 请求间隔（毫秒）
    max-limit: 100      # 单次最大爬取数量
```

#### WebClient Bean 配置

```java
@Configuration
public class WebClientConfig {
    
    @Value("${leetcode.api.url}")
    private String leetcodeApiUrl;
    
    @Value("${leetcode.api.timeout}")
    private int timeout;
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .baseUrl(leetcodeApiUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, 
                MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofMillis(timeout))
            ));
    }
}
```

### 权限配置

需要在管理端系统中添加爬虫权限：

```sql
-- 添加爬虫菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, 
    is_frame, is_cache, menu_type, visible, status, perms, icon, 
    create_by, create_time, update_by, update_time, remark)
VALUES ('题目爬虫', [OJ模块父ID], 4, 'crawler', 'oj/crawler/index', 
    1, 0, 'C', '0', '0', 'oj:crawler:view', 'download', 
    'admin', NOW(), '', NULL, 'LeetCode题目爬虫');

-- 添加爬虫执行权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, 
    is_frame, is_cache, menu_type, visible, status, perms, icon, 
    create_by, create_time, update_by, update_time, remark)
VALUES ('执行爬取', [爬虫菜单ID], 1, '', '', 
    1, 0, 'F', '0', '0', 'oj:crawler:execute', '#', 
    'admin', NOW(), '', NULL, '');
```

### 前端路由配置

管理端需要添加爬虫页面路由（如果使用后端动态菜单，则自动生成）：

```typescript
// router/routes/modules/oj.ts
{
  path: 'crawler',
  name: 'OjCrawler',
  component: () => import('@/views/oj/crawler/index.vue'),
  meta: {
    title: '题目爬虫',
    icon: 'download',
  },
}
```

## 性能优化

### 1. 批量数据库操作

```java
// 批量保存测试用例
if (CollUtil.isNotEmpty(testCases)) {
    testCaseService.saveBatch(testCases, 100);  // 每批 100 条
}

// 批量保存分类映射
if (CollUtil.isNotEmpty(categoryMaps)) {
    categoryMapService.saveBatch(categoryMaps, 100);
}
```

### 2. 数据库连接池优化

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 3. 缓存分类数据

```java
@Service
public class AcCrawlerServiceImpl implements IAcCrawlerService {
    
    // 缓存分类数据，避免重复查询
    private final Map<String, Integer> categoryCache = new ConcurrentHashMap<>();
    
    private Integer getOrCreateCategory(String tagName) {
        return categoryCache.computeIfAbsent(tagName, name -> {
            AcProblemCategory category = categoryService.getOne(
                Wrappers.lambdaQuery(AcProblemCategory.class)
                    .eq(AcProblemCategory::getName, name)
            );
            
            if (category == null) {
                category = new AcProblemCategory();
                category.setName(name);
                categoryService.save(category);
            }
            
            return category.getId();
        });
    }
}
```

### 4. 异步处理（可选）

对于大量题目的爬取，可以考虑异步处理：

```java
@Service
public class AcCrawlerServiceImpl implements IAcCrawlerService {
    
    @Async("crawlerExecutor")
    public CompletableFuture<CrawlerResultVO> executeCrawlAsync(Integer limit) {
        CrawlerResultVO result = executeCrawl(limit);
        return CompletableFuture.completedFuture(result);
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("crawlerExecutor")
    public Executor crawlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("crawler-");
        executor.initialize();
        return executor;
    }
}
```

## 安全考虑

### 1. 请求频率限制

```java
@Component
public class RateLimiter {
    
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long MIN_INTERVAL_MS = 500;
    
    public void checkAndWait(String key) throws InterruptedException {
        Long lastTime = lastRequestTime.get(key);
        if (lastTime != null) {
            long elapsed = System.currentTimeMillis() - lastTime;
            if (elapsed < MIN_INTERVAL_MS) {
                Thread.sleep(MIN_INTERVAL_MS - elapsed);
            }
        }
        lastRequestTime.put(key, System.currentTimeMillis());
    }
}
```

### 2. 输入验证

```java
@PostMapping("/execute")
public R<CrawlerResultVO> execute(@RequestBody CrawlerRequestBO request) {
    Integer limit = request.getLimit();
    
    // 验证输入
    if (limit == null || limit <= 0) {
        return R.fail("爬取数量必须大于 0");
    }
    if (limit > 100) {
        return R.fail("单次爬取数量不能超过 100");
    }
    
    // 执行爬取
    return R.ok(crawlerService.executeCrawl(limit));
}
```

### 3. 敏感信息保护

```java
// 不记录完整的 API 响应到日志
log.debug("API 响应状态: {}, 题目数量: {}", 
    response.getStatusCode(), 
    response.getData().size());

// 而不是
// log.debug("API 响应: {}", response);  // 可能包含敏感信息
```

## 监控和日志

### 1. 关键指标监控

```java
@Component
public class CrawlerMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordCrawlSuccess(int count) {
        meterRegistry.counter("crawler.success", "type", "problem")
            .increment(count);
    }
    
    public void recordCrawlFailure(int count) {
        meterRegistry.counter("crawler.failure", "type", "problem")
            .increment(count);
    }
    
    public void recordCrawlDuration(long durationMs) {
        meterRegistry.timer("crawler.duration")
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

### 2. 结构化日志

```java
@Slf4j
public class AcCrawlerServiceImpl implements IAcCrawlerService {
    
    @Override
    public CrawlerResultVO executeCrawl(Integer limit) {
        MDC.put("operation", "leetcode-crawl");
        MDC.put("limit", String.valueOf(limit));
        
        try {
            log.info("开始爬取操作");
            // 执行爬取
            log.info("爬取完成", 
                kv("success", result.getSuccessCount()),
                kv("skip", result.getSkipCount()),
                kv("fail", result.getFailCount()));
            return result;
        } finally {
            MDC.clear();
        }
    }
}
```

## 总结

本设计文档详细描述了 LeetCode 题目爬虫功能的技术实现方案，包括：

1. **系统架构**：清晰的组件划分和职责定义
2. **API 设计**：完整的接口定义和数据模型
3. **LeetCode API 集成**：GraphQL 查询和客户端实现
4. **数据处理流程**：从获取到存储的完整流程
5. **错误处理**：全面的错误分类和处理策略
6. **正确性属性**：12 个核心属性确保系统正确性
7. **测试策略**：单元测试和属性测试相结合
8. **性能优化**：批量操作、缓存、异步处理
9. **安全考虑**：频率限制、输入验证、信息保护
10. **监控日志**：指标监控和结构化日志

该设计方案确保了爬虫功能的稳定性、可靠性和可维护性，为后续的开发和测试提供了清晰的指导。

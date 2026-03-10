# 实现计划：LeetCode 题目爬虫

## 概述

本任务列表将 LeetCode 题目爬虫功能拆分为可执行的开发任务。该功能通过调用 LeetCode 的公开 GraphQL API 获取题目数据，并将数据映射后导入到 AlpenCode 平台的题库中。实现包括后端服务、前端管理页面、数据模型、API 客户端、测试用例解析器等核心组件。

## 任务列表

- [ ] 1. 创建数据模型和 DTO 类
  - [x] 1.1 创建 DTO 类（LeetCodeProblemDTO、TestCaseDTO）
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/domain/dto` 包下创建 `LeetCodeProblemDTO` 类，包含 titleSlug、title、description、difficulty、tags、exampleTestCases 字段
    - 创建 `TestCaseDTO` 类，包含 input、expectedOutput 字段
    - 使用 Lombok 的 @Data 注解
    - _需求：1.1, 1.2, 2.1_

  - [x] 1.2 创建 VO 类（CrawlerResultVO、FailedProblemVO）
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/domain/vo` 包下创建 `CrawlerResultVO` 类，包含 successCount、skipCount、failCount、failedProblems 字段
    - 创建 `FailedProblemVO` 类，包含 title、reason 字段
    - 添加 JavaDoc 注释说明每个字段的含义
    - _需求：4.3, 5.3_

  - [x] 1.3 创建 BO 类（CrawlerRequestBO）
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/domain/bo` 包下创建 `CrawlerRequestBO` 类
    - 包含 limit 字段（默认值 20）
    - 添加参数校验注解（@Min、@Max）
    - _需求：5.1_

- [ ] 2. 实现 LeetCode API 客户端
  - [x] 2.1 创建 WebClient 配置类
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/config` 包下创建 `WebClientConfig` 类
    - 配置 WebClient.Builder Bean，设置 baseUrl、headers、timeout
    - 从 application.yml 读取配置参数（leetcode.api.url、leetcode.api.timeout）
    - _需求：1.1, 1.3_

  - [x] 2.2 实现 LeetCodeApiClient 组件
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/client` 包下创建 `LeetCodeApiClient` 类
    - 实现 `fetchProblemList(int limit)` 方法，构建 GraphQL 查询并发送请求
    - 实现 `fetchProblemDetail(String titleSlug)` 方法，获取题目详情
    - 添加请求延迟控制（500ms）
    - 添加错误处理和重试机制（使用 @Retryable）
    - _需求：1.1, 1.2, 1.3, 2.1, 2.2_

  - [x] 2.3 编写 LeetCodeApiClient 单元测试
    - 测试 fetchProblemList 方法的正常场景和错误场景
    - 测试 fetchProblemDetail 方法的网络超时处理
    - 使用 MockWebServer 模拟 LeetCode API 响应
    - _需求：1.3, 2.3_

- [ ] 3. 实现难度映射器
  - [x] 3.1 创建 DifficultyMapper 组件
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/mapper` 包下创建 `DifficultyMapper` 类
    - 实现 `mapDifficulty(String leetcodeDifficulty)` 方法，将 Easy/Medium/Hard 映射为 1/2/3
    - 实现 `reverseDifficulty(Integer difficulty)` 方法，用于属性测试的双射验证
    - 添加异常处理（未知难度值）
    - _需求：3.1_

  - [x] 3.2 编写难度映射器属性测试
    - **属性 6：难度映射双射性**
    - **验证需求：3.1**
    - 使用 JUnit QuickCheck 验证映射后再反向映射得到原始值
    - 创建 DifficultyGenerator 生成随机难度值
    - _需求：3.1_

- [ ] 4. 实现测试用例解析器
  - [x] 4.1 创建 TestCaseParser 组件
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/parser` 包下创建 `TestCaseParser` 类
    - 实现 `parseExamples(String htmlContent)` 方法，从 HTML 中提取示例测试用例
    - 使用 Jsoup 解析 HTML，查找"示例 1"、"示例 2"等标题
    - 提取"输入："和"输出："后的内容
    - 支持多组示例解析
    - 添加错误处理（解析失败时记录警告日志并跳过）
    - _需求：6.1, 6.2, 7.1, 7.2, 7.4_

  - [x] 4.2 编写测试用例解析器单元测试
    - 测试单个示例解析
    - 测试多个示例解析
    - 测试格式不符合预期的情况
    - 测试空 HTML 内容
    - _需求：7.1, 7.2, 7.4_

  - [x] 4.3 编写测试用例解析器属性测试
    - **属性 12：示例解析 Round-Trip**
    - **验证需求：7.3**
    - 验证解析后格式化输出再解析得到语义等价的数据
    - _需求：7.3_

- [ ] 5. 实现爬虫服务核心逻辑
  - [x] 5.1 创建爬虫服务接口和实现类
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/service` 包下创建 `IAcCrawlerService` 接口
    - 定义 `executeCrawl(Integer limit)` 方法
    - 在 `ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/crawler/service/impl` 包下创建 `AcCrawlerServiceImpl` 实现类
    - 注入依赖：LeetCodeApiClient、TestCaseParser、DifficultyMapper、IAcProblemService、IAcProblemCategoryService、IAcTestCaseService
    - _需求：1.1, 2.1, 3.2, 4.1_

  - [x] 5.2 实现主流程方法 executeCrawl
    - 初始化 CrawlerResultVO 结果对象
    - 调用 LeetCodeApiClient 获取题目列表
    - 遍历题目列表，调用 processSingleProblem 处理每道题目
    - 捕获异常并记录失败信息
    - 返回执行结果
    - _需求：1.1, 2.1, 4.3_

  - [x] 5.3 实现单题处理方法 processSingleProblem
    - 检查题目是否重复（根据 title 查询数据库）
    - 如果重复则跳过，skipCount++
    - 调用 LeetCodeApiClient 获取题目详情
    - 调用 TestCaseParser 解析示例测试用例
    - 调用 mapToProblem 映射题目数据
    - 使用 @Transactional 注解确保事务完整性
    - 保存题目、处理分类标签、保存测试用例
    - 成功后 successCount++，失败后 failCount++ 并记录失败信息
    - _需求：2.1, 2.3, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 6.1, 6.2_

  - [x] 5.4 实现辅助方法（isDuplicate、mapToProblem、processTags、saveTestCases）
    - `isDuplicate(String title)`：检查题目是否已存在
    - `mapToProblem(LeetCodeProblemDTO dto)`：将 DTO 映射为 AcProblem 实体
    - `processTags(Integer problemId, List<String> tags)`：处理分类标签，查找或创建分类，建立关联
    - `saveTestCases(Integer problemId, List<TestCaseDTO> testCases)`：保存示例测试用例，设置 is_sample=1
    - _需求：3.1, 3.2, 3.3, 3.4, 4.1, 6.2_

  - [-] 5.5 编写爬虫服务单元测试
    - 测试空题目列表处理
    - 测试重复题目跳过
    - 测试单题失败不影响其他题目
    - 测试事务回滚场景
    - 使用 @MockBean 模拟依赖组件
    - _需求：1.4, 2.3, 3.5, 4.1, 4.2_

  - [ ] 5.6 编写爬虫服务属性测试
    - **属性 1：API 调用完整性**
    - **验证需求：1.1, 2.1**
    - **属性 5：部分失败容错**
    - **验证需求：2.3**
    - **属性 7：数据存储事务完整性**
    - **验证需求：3.2, 3.3, 3.4, 3.5**
    - **属性 8：去重机制有效性**
    - **验证需求：4.1, 4.2**
    - **属性 9：统计数据一致性**
    - **验证需求：4.3**
    - 使用 JUnit QuickCheck 生成随机测试数据
    - 创建 ProblemGenerator 生成随机题目数据
    - _需求：1.1, 2.1, 2.3, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3_

- [-] 6. 实现爬虫控制器
  - [x] 6.1 创建 AcCrawlerController 类
    - 在 `ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/crawler/controller` 包下创建 `AcCrawlerController` 类
    - 继承 BaseController
    - 注入 IAcCrawlerService
    - 添加 @RequestMapping("/oj/crawler") 注解
    - _需求：5.1, 5.2_

  - [x] 6.2 实现 execute 接口方法
    - 添加 `@PostMapping("/execute")` 注解
    - 添加 `@SaCheckPermission("oj:crawler:execute")` 权限控制
    - 添加 `@Log` 注解记录操作日志
    - 接收 CrawlerRequestBO 参数
    - 验证 limit 参数（1-100 之间）
    - 调用 crawlerService.executeCrawl 执行爬取
    - 返回 R<CrawlerResultVO> 响应
    - 添加异常处理
    - _需求：5.1, 5.2, 5.3_

  - [ ] 6.3 编写控制器单元测试
    - 测试正常爬取场景
    - 测试参数验证（limit 超出范围）
    - 测试权限控制（无权限用户访问）
    - 使用 MockMvc 模拟 HTTP 请求
    - _需求：5.1, 5.2, 5.3_

  - [ ] 6.4 编写控制器属性测试
    - **属性 10：权限控制有效性**
    - **验证需求：5.2**
    - **属性 11：响应格式完整性**
    - **验证需求：5.3**
    - _需求：5.2, 5.3_

- [x] 7. Checkpoint - 确保后端核心功能测试通过
  - 运行所有后端单元测试和属性测试，确保测试通过
  - 如有问题请询问用户

- [x] 8. 配置文件和依赖
  - [x] 8.1 添加 Jsoup 依赖到 pom.xml
    - 在 `ruoyi-modules-api/ruoyi-system-api/pom.xml` 中添加 Jsoup 依赖（用于 HTML 解析）
    - 版本：1.15.3
    - _需求：7.1_

  - [x] 8.2 添加 JUnit QuickCheck 依赖到 pom.xml
    - 添加 junit-quickcheck-core 和 junit-quickcheck-generators 依赖
    - 版本：1.0
    - scope：test
    - _需求：属性测试_

  - [x] 8.3 添加 Spring Retry 依赖到 pom.xml
    - 添加 spring-retry 和 spring-aspects 依赖
    - 用于 API 请求重试机制
    - _需求：1.3_

  - [x] 8.4 配置 application.yml
    - 在 `ruoyi-admin/src/main/resources/application.yml` 中添加 leetcode 配置节
    - 配置 api.url（https://leetcode.cn/graphql/）
    - 配置 api.timeout（10000ms）
    - 配置 api.retry.max-attempts（3）
    - 配置 api.retry.delay（2000ms）
    - 配置 crawler.request-delay（500ms）
    - 配置 crawler.max-limit（100）
    - _需求：1.1, 1.3, 2.2_

- [x] 9. 数据库权限配置
  - [x] 9.1 创建菜单权限 SQL 脚本
    - 在 `ruoyi-ai/script/sql/update/` 目录下创建 `ac_crawler_menu.sql` 文件
    - 插入"题目爬虫"菜单记录到 sys_menu 表（menu_type='C'，perms='oj:crawler:view'）
    - 插入"执行爬取"权限记录到 sys_menu 表（menu_type='F'，perms='oj:crawler:execute'）
    - 设置正确的 parent_id（OJ 模块的菜单 ID）
    - _需求：5.2_

  - [x] 9.2 执行 SQL 脚本
    - 在数据库中执行 ac_crawler_menu.sql 脚本
    - 验证菜单和权限是否正确创建
    - _需求：5.2_

- [x] 10. 前端 API 定义和类型
  - [x] 10.1 创建 API 类型定义文件
    - 在 `ruoyi-admin/apps/web-antd/src/api/oj/crawler/` 目录下创建 `model.d.ts` 文件
    - 定义 CrawlerRequestBO 接口（limit 字段）
    - 定义 CrawlerResultVO 接口（successCount、skipCount、failCount、failedProblems 字段）
    - 定义 FailedProblemVO 接口（title、reason 字段）
    - _需求：5.1, 5.3_

  - [x] 10.2 创建 API 请求函数
    - 在 `ruoyi-admin/apps/web-antd/src/api/oj/crawler/` 目录下创建 `index.ts` 文件
    - 定义 Api 枚举（Execute = '/oj/crawler/execute'）
    - 实现 `executeCrawler(params: CrawlerRequestBO)` 函数
    - 使用 defHttp.post 发送 POST 请求
    - 返回类型为 Promise<CrawlerResultVO>
    - _需求：5.1_

- [x] 11. 前端爬虫管理页面
  - [x] 11.1 创建爬虫管理页面主文件
    - 在 `ruoyi-admin/apps/web-antd/src/views/oj/crawler/` 目录下创建 `index.vue` 文件
    - 使用 `<script setup lang="ts">` 语法
    - 导入必要的组件和 API 函数
    - _需求：8.1, 8.2_

  - [x] 11.2 实现爬取表单组件
    - 使用 a-card 包裹整个页面，标题为"LeetCode 题目爬虫"
    - 使用 a-form 创建表单，layout="inline"
    - 添加 a-input-number 输入框，绑定 formState.limit，范围 1-100，默认值 20
    - 添加"开始爬取"按钮，绑定 handleCrawl 方法
    - 爬取进行中时禁用输入框和按钮
    - _需求：8.2, 8.3, 8.8_

  - [x] 11.3 实现进度显示组件
    - 使用 a-spin 显示加载状态
    - 使用 a-alert 显示"爬取进行中"提示信息
    - 仅在 crawling 为 true 时显示
    - _需求：8.5_

  - [x] 11.4 实现结果展示组件
    - 使用 a-row 和 a-col 布局，显示三个统计数据
    - 使用 a-statistic 组件显示成功数、跳过数、失败数
    - 使用不同颜色区分（成功=绿色、跳过=橙色、失败=红色）
    - 添加图标（CheckCircleOutlined、MinusCircleOutlined、CloseCircleOutlined）
    - 仅在 result 不为 null 时显示
    - _需求：8.6_

  - [x] 11.5 实现失败题目列表组件
    - 使用 a-table 显示失败题目列表
    - 定义 failedColumns（题目标题、失败原因）
    - 绑定 result.failedProblems 数据源
    - 仅在 failedProblems 不为空时显示
    - _需求：8.7_

  - [x] 11.6 实现 handleCrawl 方法
    - 验证 limit 参数（1-100 之间）
    - 设置 crawling 为 true，清空 result
    - 调用 executeCrawler API 函数
    - 成功后更新 result，显示成功消息
    - 失败后显示错误消息
    - finally 块中设置 crawling 为 false
    - _需求：8.4, 8.5, 8.6, 8.7_

  - [x] 11.7 添加页面样式
    - 使用 `<style scoped lang="less">` 定义样式
    - 设置 progress-section 和 result-section 的 margin-top
    - _需求：8.1_

- [x] 12. 前端路由配置
  - [x] 12.1 配置爬虫页面路由（如果需要手动配置）
    - 如果管理端使用后端动态菜单，则路由会自动生成，无需手动配置
    - 如果需要手动配置，在 `ruoyi-admin/apps/web-antd/src/router/routes/modules/oj.ts` 中添加 crawler 路由
    - path: 'crawler'，name: 'OjCrawler'，component: () => import('@/views/oj/crawler/index.vue')
    - meta: { title: '题目爬虫', icon: 'download' }
    - _需求：8.1_

- [x] 13. Checkpoint - 确保前端功能正常
  - 启动前端开发服务器，访问爬虫管理页面
  - 测试输入框、按钮、进度显示、结果展示是否正常
  - 如有问题请询问用户

- [x] 14. 集成测试和联调
  - [x] 14.1 端到端测试
    - 启动后端服务和前端应用
    - 在管理端登录，访问"题目爬虫"页面
    - 输入爬取数量（如 5），点击"开始爬取"
    - 验证进度显示是否正常
    - 验证结果展示是否正确（成功数、跳过数、失败数）
    - 验证失败题目列表是否显示
    - _需求：所有需求_

  - [x] 14.2 重复爬取测试
    - 执行第一次爬取，记录成功数
    - 立即执行第二次爬取，验证跳过数是否等于第一次的成功数
    - 验证数据库中没有重复题目
    - _需求：4.1, 4.2_

  - [x] 14.3 错误场景测试
    - 断开网络连接，测试网络错误处理
    - 输入超出范围的 limit 值，测试参数验证
    - 使用无权限用户访问接口，测试权限控制
    - _需求：1.3, 5.2, 5.3_

  - [x] 14.4 性能测试
    - 爬取 50 道题目，记录耗时
    - 验证请求频率控制是否生效（每次请求间隔 ≥ 500ms）
    - 检查数据库连接池使用情况
    - _需求：2.2_

- [x] 15. 文档和收尾
  - [x] 15.1 更新 README 或开发文档
    - 记录爬虫功能的使用方法
    - 记录配置参数说明
    - 记录权限配置步骤
    - _需求：所有需求_

  - [x] 15.2 代码审查和优化
    - 检查代码规范（命名、注释、格式）
    - 优化异常处理和日志记录
    - 移除调试代码和无用注释
    - _需求：所有需求_

## 注意事项

- 标记 `*` 的任务为可选任务，可以跳过以加快 MVP 开发
- 每个任务都引用了具体的需求编号，确保可追溯性
- Checkpoint 任务用于阶段性验证，确保增量开发的正确性
- 属性测试验证通用正确性属性，单元测试验证具体示例和边界情况
- 所有数据库操作使用事务确保数据一致性
- 请求频率控制避免触发 LeetCode 的限流机制

# 需求文档：LeetCode 题目爬虫

## 简介

本功能为 AlpenCode 平台提供 LeetCode 题目数据爬取能力，通过调用 LeetCode 的公开 GraphQL API 获取题目信息（标题、描述、难度、分类标签等），并将数据映射后批量导入到 AlpenCode 的题库（ac_problem、ac_problem_category、ac_problem_category_map）中。该功能作为管理端的后台工具，由管理员触发执行。

## 术语表

- **Crawler_Service**：LeetCode 爬虫服务，负责从 LeetCode 获取题目数据并存入 AlpenCode 题库
- **LeetCode_API**：LeetCode 网站提供的公开 GraphQL 接口（https://leetcode.cn/graphql/），用于查询题目列表和题目详情
- **Problem_Store**：AlpenCode 平台的题库存储，包括 ac_problem、ac_problem_category、ac_problem_category_map 三张表
- **Category_Store**：AlpenCode 平台的分类存储，即 ac_problem_category 表
- **Difficulty_Mapper**：难度值映射组件，将 LeetCode 的难度标识（Easy/Medium/Hard）转换为 AlpenCode 的难度值（1/2/3）
- **Admin_User**：AlpenCode 管理端的管理员用户，拥有触发爬虫的权限
- **Title_Slug**：LeetCode 题目的唯一标识符（URL 友好的英文标题），用于查询题目详情
- **Admin_Frontend**：AlpenCode 管理端前端应用（ruoyi-admin/apps/web-antd/），基于 Vue 3 + Ant Design Vue + Vben Admin 框架
- **Crawler_Page**：管理端前端中的爬虫管理页面，提供可视化的爬虫操作界面

## 需求

### 需求 1：获取 LeetCode 题目列表

**用户故事：** 作为管理员，我希望能从 LeetCode 获取题目列表数据，以便选择性地导入题目到 AlpenCode 题库。

#### 验收标准

1. WHEN Admin_User 触发爬取操作并指定题目数量时，THE Crawler_Service SHALL 通过 LeetCode_API 的 GraphQL 接口查询指定数量的题目列表
2. THE Crawler_Service SHALL 从题目列表响应中提取每道题目的 Title_Slug、标题、难度和分类标签
3. IF LeetCode_API 返回错误或网络连接失败，THEN THE Crawler_Service SHALL 记录错误日志并返回包含错误原因的失败响应
4. IF LeetCode_API 返回的题目列表为空，THEN THE Crawler_Service SHALL 返回提示信息告知无可用题目

### 需求 2：获取 LeetCode 题目详情

**用户故事：** 作为管理员，我希望能获取每道 LeetCode 题目的完整描述内容，以便导入后用户可以在 AlpenCode 平台上阅读完整题目。

#### 验收标准

1. WHEN Crawler_Service 获取到题目列表后，THE Crawler_Service SHALL 逐个通过 Title_Slug 调用 LeetCode_API 查询每道题目的详细描述（HTML 格式）
2. THE Crawler_Service SHALL 在连续请求之间设置合理的间隔时间（不低于 500ms），避免触发 LeetCode 的请求频率限制
3. IF 单道题目详情请求失败，THEN THE Crawler_Service SHALL 记录该题目的错误信息并跳过该题目，继续处理剩余题目

### 需求 3：数据映射与存储

**用户故事：** 作为管理员，我希望爬取的 LeetCode 题目数据能正确映射并存入 AlpenCode 题库，以便用户可以在平台上练习这些题目。

#### 验收标准

1. THE Difficulty_Mapper SHALL 将 LeetCode 的难度值映射为 AlpenCode 的难度值：Easy 映射为 1，Medium 映射为 2，Hard 映射为 3
2. WHEN 题目数据准备就绪时，THE Crawler_Service SHALL 将题目标题、描述、难度、默认时间限制（1000ms）和默认内存限制（256MB）存入 Problem_Store 的 ac_problem 表
3. WHEN 题目携带分类标签时，THE Crawler_Service SHALL 检查 Category_Store 中是否已存在同名分类：若存在则复用，若不存在则新建分类记录
4. WHEN 题目和分类均已存入后，THE Crawler_Service SHALL 在 ac_problem_category_map 表中建立题目与分类的关联关系
5. THE Crawler_Service SHALL 在单个数据库事务中完成单道题目的存储操作（包括题目、分类、关联关系），保证数据一致性

### 需求 4：去重机制

**用户故事：** 作为管理员，我希望重复爬取时不会产生重复题目，以保持题库数据的整洁。

#### 验收标准

1. WHEN 导入题目前，THE Crawler_Service SHALL 根据题目标题检查 Problem_Store 中是否已存在相同标题的题目
2. IF Problem_Store 中已存在相同标题的题目，THEN THE Crawler_Service SHALL 跳过该题目并在结果中标记为"已跳过（重复）"
3. WHEN 爬取操作完成后，THE Crawler_Service SHALL 返回执行摘要，包含：成功导入数量、跳过数量（重复）、失败数量

### 需求 5：管理端接口与权限控制

**用户故事：** 作为管理员，我希望通过管理端的 API 接口触发爬虫操作，并且只有管理员才能执行此操作。

#### 验收标准

1. THE Crawler_Service SHALL 提供一个 HTTP POST 接口供 Admin_User 触发爬取操作，接口接受参数：爬取题目数量（默认 20）
2. THE Crawler_Service SHALL 通过 Sa-Token 的权限注解限制该接口仅允许拥有 `oj:crawler:execute` 权限的 Admin_User 访问
3. WHEN 爬取操作完成后，THE Crawler_Service SHALL 返回 JSON 格式的执行结果，包含成功数量、跳过数量、失败数量和失败题目列表

### 需求 6：LeetCode 示例测试用例导入

**用户故事：** 作为管理员，我希望爬取题目时能同时获取 LeetCode 提供的示例测试用例，以便用户在 AlpenCode 平台上可以使用 Run Code 功能调试代码。

#### 验收标准

1. WHEN Crawler_Service 获取题目详情时，THE Crawler_Service SHALL 从题目描述中解析出示例输入和示例输出数据
2. WHEN 示例测试用例解析成功时，THE Crawler_Service SHALL 将解析出的测试用例存入 ac_test_case 表，并设置 is_sample 为 1（公开样例）
3. IF 题目描述中未包含可解析的示例测试用例，THEN THE Crawler_Service SHALL 跳过测试用例导入并记录日志，题目本身的导入不受影响

### 需求 7：示例测试用例解析

**用户故事：** 作为管理员，我希望爬虫能准确解析 LeetCode 题目中的示例输入输出，以便导入的测试用例数据是正确可用的。

#### 验收标准

1. THE Crawler_Service SHALL 从 LeetCode 题目描述的 HTML 内容中识别并提取 "输入" 和 "输出" 格式的示例数据
2. THE Crawler_Service SHALL 支持解析多组示例（如示例 1、示例 2、示例 3）
3. FOR ALL 成功解析的示例测试用例，解析后再格式化输出 SHALL 保持输入输出数据的语义等价性（round-trip 属性）
4. IF 示例数据格式不符合预期模式，THEN THE Crawler_Service SHALL 跳过该示例并记录警告日志

### 需求 8：管理端爬虫管理页面

**用户故事：** 作为管理员，我希望在管理端前端有一个独立的爬虫管理页面，以便我可以在页面上直接操作爬虫、查看爬取进度和结果。

#### 验收标准

1. THE Admin_Frontend SHALL 在 OJ 模块下提供一个"题目爬虫"菜单项或 Tab 页
2. THE Crawler_Page SHALL 提供一个输入框让 Admin_User 指定爬取题目数量（默认值为 20）
3. THE Crawler_Page SHALL 提供一个"开始爬取"按钮，点击后触发爬虫服务
4. WHEN Admin_User 点击"开始爬取"按钮时，THE Crawler_Page SHALL 调用后端爬虫接口并显示加载状态
5. WHEN 爬取操作进行中时，THE Crawler_Page SHALL 实时显示爬取进度信息，包括：已处理题目数、成功数、跳过数、失败数
6. WHEN 爬取操作完成后，THE Crawler_Page SHALL 显示完整的执行摘要，包括：总成功数、总跳过数、总失败数
7. IF 爬取过程中有题目失败，THEN THE Crawler_Page SHALL 显示失败题目列表，每条记录包含题目标题和失败原因
8. THE Crawler_Page SHALL 在爬取操作进行中禁用"开始爬取"按钮，防止重复触发

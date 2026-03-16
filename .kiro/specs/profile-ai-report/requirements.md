# 需求文档：个人中心 + AI 能力总结报告

## 简介

为 AlpenCode 用户端实现个人中心模块，包含个人信息管理和 AI 能力总结报告两大功能。个人信息管理允许用户修改用户名、邮箱和密码。AI 能力总结报告以全屏滚动卡片形式展示用户的刷题数据和 AI 生成的个性化分析，类似年终总结的沉浸式体验。报告由用户主动触发生成，异步调用 AI 后存入数据库。

## 术语表

- **ProfilePage**：个人中心页面，展示用户基本信息、提供信息编辑入口和 AI 报告入口
- **AiReport**：AI 能力总结报告，基于用户刷题数据由 AI 生成的个性化分析报告
- **ReportPage**：AI 报告展示页面，全屏滚动卡片式沉浸体验
- **ac_ai_report**：AI 报告存储表，保存生成的报告内容和统计快照
- **UserStats**：用户刷题统计数据，从 ac_submit 聚合得出（总提交、通过数、各难度分布、错误类型分布等）
- **ECharts**：前端图表库，用于渲染雷达图、环形图、柱状图等可视化内容

## 需求

### 需求 1：个人中心页面

**用户故事：** 作为已登录用户，我希望在个人中心查看和管理我的基本信息，以便维护我的账号资料。

#### 验收标准

1. WHEN 已登录用户导航到个人中心页面，THE ProfilePage SHALL 展示当前用户的用户名、邮箱和注册时间
2. WHEN 用户点击"修改信息"，THE ProfilePage SHALL 展示编辑表单，允许修改用户名和邮箱
3. WHEN 用户提交修改后的用户名和邮箱，THE 后端 SHALL 校验用户名唯一性（排除自身），更新 ac_user 表并返回成功响应
4. WHEN 修改的用户名已被其他用户占用，THE 后端 SHALL 返回"用户名已存在"错误
5. WHEN 用户点击"修改密码"，THE ProfilePage SHALL 展示密码修改表单，包含旧密码、新密码、确认新密码三个字段
6. WHEN 用户提交密码修改请求，THE 后端 SHALL 校验旧密码正确性，对新密码进行 BCrypt 哈希后更新 ac_user 表
7. WHEN 旧密码校验失败，THE 后端 SHALL 返回"旧密码错误"错误信息
8. WHEN 新密码与确认密码不一致，THE 前端 SHALL 阻止提交并提示"两次密码不一致"

### 需求 2：用户刷题统计数据聚合

**用户故事：** 作为系统，我需要从提交记录中聚合用户的刷题统计数据，为 AI 报告生成提供数据支撑。

#### 验收标准

1. THE 后端 SHALL 提供接口返回当前用户的统计数据，包含：总提交次数、通过题目数（去重）、总体通过率
2. THE 统计数据 SHALL 包含各难度通过题目数：简单通过数、中等通过数、困难通过数
3. THE 统计数据 SHALL 包含各分类维度的通过题目数（基于 ac_problem_category_map 关联）
4. THE 统计数据 SHALL 包含错误类型分布：WA 次数、TLE 次数、RE 次数、CE 次数
5. THE 统计数据 SHALL 包含用户注册天数（从 ac_user.created_at 到当前日期）

### 需求 3：AI 报告生成

**用户故事：** 作为已登录用户，我希望在个人中心触发生成 AI 能力报告，AI 基于我的刷题数据生成个性化分析。

#### 验收标准

1. WHEN 用户在个人中心点击"生成 AI 报告"按钮，THE 后端 SHALL 异步聚合用户统计数据并调用 AI 生成报告
2. THE 后端 SHALL 将生成的报告内容和统计数据快照存入 ac_ai_report 表
3. WHEN AI 报告正在生成中（status=0），THE 前端 SHALL 展示生成中状态，禁止重复触发
4. WHEN AI 报告生成完成（status=1），THE 前端 SHALL 允许用户查看报告
5. WHEN AI 报告生成失败（status=2），THE 前端 SHALL 提示失败并允许重新生成
6. THE 后端 SHALL 提供查询当前用户最新报告的接口，返回报告状态和内容

### 需求 4：AI 报告展示页面（全屏滚动卡片）

**用户故事：** 作为已登录用户，我希望以沉浸式全屏滚动的方式查看我的 AI 能力报告，获得类似年终总结的体验。

#### 验收标准

1. WHEN 用户点击查看报告，THE ReportPage SHALL 以全屏滚动方式展示多个卡片区域
2. THE ReportPage 第一屏 SHALL 展示总览信息：注册天数、总提交次数、通过题目数、总体通过率，以及 AI 生成的一句个性化评语
3. THE ReportPage 第二屏 SHALL 展示难度分布：简单/中等/困难的通过数量，使用 ECharts 环形图或柱状图可视化，附 AI 点评
4. THE ReportPage 第三屏 SHALL 展示分类能力：按题目分类的通过情况，使用 ECharts 雷达图可视化，标出擅长和薄弱分类
5. THE ReportPage 第四屏 SHALL 展示错误类型分析：WA/TLE/RE/CE 各占比，使用 ECharts 饼图可视化，附 AI 分析
6. THE ReportPage 第五屏 SHALL 展示 AI 综合评价：完整的能力分析、弱点分析和学习建议
7. THE ReportPage SHALL 支持鼠标滚轮或触摸滑动在各屏之间平滑切换
8. THE ReportPage SHALL 在每屏切换时有过渡动画效果

### 需求 5：接口路径规范

**用户故事：** 作为后端开发者，我希望个人中心和 AI 报告相关接口遵循统一的路径规范。

#### 验收标准

1. THE 后端 SHALL 将修改用户信息接口暴露在 `PUT /ac/profile/info` 路径
2. THE 后端 SHALL 将修改密码接口暴露在 `PUT /ac/profile/password` 路径
3. THE 后端 SHALL 将获取用户统计数据接口暴露在 `GET /ac/profile/stats` 路径
4. THE 后端 SHALL 将触发生成 AI 报告接口暴露在 `POST /ac/profile/report/generate` 路径
5. THE 后端 SHALL 将查询最新报告接口暴露在 `GET /ac/profile/report/latest` 路径

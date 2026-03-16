# 实现计划：个人中心 + AI 能力总结报告

## 概述

为 AlpenCode 用户端实现个人中心模块（修改用户名/邮箱/密码）和 AI 能力总结报告（全屏滚动卡片式，ECharts 可视化）。复用现有 AcUserDTO/AcUserVo/IAcUserService 处理用户信息修改；新增 ac_ai_report 表、统计聚合逻辑和 AI 报告生成。

### 复用现有类（不新建）
- `AcUserDTO` — 修改用户信息直接复用（已有 id, username, email）
- `AcUserVo` — 用户信息展示直接复用
- `IAcUserService.updateByDTO()` — 修改用户信息直接调用
- `AcUserMapper` / `AcSubmitMapper` — 直接注入使用

### 必须新建的类
- `AcPasswordUpdateDTO` — 密码修改（oldPassword + newPassword）
- `AcUserStatsVo` — 刷题统计数据（全新结构）
- `AcAiReport` 实体 + `AcAiReportMapper` — 新表
- `AcAiReportVo` — 报告视图
- `IAcProfileService` + `AcProfileServiceImpl` — 个人中心业务
- `AcProfileController` — 个人中心控制器

## 任务

- [x] 1. 数据库表和实体类
  - [x] 1.1 创建 ac_ai_report 表
    - 在 `AlpenCode.sql` 末尾追加建表语句
    - 字段：id, user_id, status(0生成中/1完成/2失败), stats_snapshot(JSON), report_content(JSON), is_delete, created_at, updated_at
    - _需求: 3.2_

  - [x] 1.2 创建 AcAiReport 实体类
    - 文件：`ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/AcAiReport.java`
    - _需求: 3.2_

  - [x] 1.3 创建 AcAiReportMapper
    - 文件：`ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/mapper/AcAiReportMapper.java`
    - 继承 BaseMapper<AcAiReport>
    - _需求: 3.2_

- [x] 2. 后端 DTO/VO（仅新建必要的）
  - [x] 2.1 创建 AcPasswordUpdateDTO
    - 字段：oldPassword(@NotBlank), newPassword(@NotBlank, @Size(6,20))
    - 文件：`ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/AcPasswordUpdateDTO.java`
    - _需求: 1.5, 1.6_

  - [x] 2.2 创建 AcUserStatsVo
    - 字段：totalSubmissions, solvedCount, acceptRate, easyCount, mediumCount, hardCount, waCount, tleCount, reCount, ceCount, daysSinceJoin, categoryStats(List<CategoryStat>)
    - 内部静态类 CategoryStat：name, count
    - 文件：`ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/vo/AcUserStatsVo.java`
    - _需求: 2.1-2.5_

  - [x] 2.3 创建 AcAiReportVo
    - 字段：id, status, statsSnapshot, reportContent, createdAt
    - 文件：`ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/vo/AcAiReportVo.java`
    - _需求: 3.6_

- [x] 3. 后端 Service 层
  - [x] 3.1 创建 IAcProfileService 接口
    - 方法：updateInfo(Integer userId, AcUserDTO dto), updatePassword(Integer userId, AcPasswordUpdateDTO dto), getStats(Integer userId), generateReport(Integer userId), getLatestReport(Integer userId)
    - 文件：`ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/service/oj/IAcProfileService.java`
    - _需求: 1.2, 1.5, 2.1, 3.1, 3.6_

  - [x] 3.2 创建 AcProfileServiceImpl 实现类
    - 注入：IAcUserService, AcUserMapper, AcSubmitMapper, AcAiReportMapper, ChatClient
    - **updateInfo**：校验用户名唯一性（排除自身 id），然后调用 IAcUserService.updateByDTO(dto)
    - **updatePassword**：查询当前密码哈希 → BCrypt.checkpw 旧密码 → BCrypt.hashpw 新密码 → 更新
    - **getStats**：聚合 SQL 查询统计数据，计算注册天数，组装 AcUserStatsVo
    - **generateReport**：getStats → 插入 ac_ai_report(status=0) → @Async 调 ChatClient → 成功更新 status=1 / 失败更新 status=2
    - **getLatestReport**：查询最新一条报告
    - 文件：`ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/service/oj/impl/AcProfileServiceImpl.java`
    - _需求: 1.3, 1.4, 1.6, 1.7, 2.1-2.5, 3.1-3.6_

- [x] 4. 后端 Controller 层
  - [x] 4.1 创建 AcProfileController
    - `@RestController` + `@RequestMapping("/ac/profile")`
    - `PUT /info`：接收 @Valid @RequestBody AcUserDTO（复用），调用 profileService.updateInfo
    - `PUT /password`：接收 @Valid @RequestBody AcPasswordUpdateDTO，调用 profileService.updatePassword
    - `GET /stats`：调用 profileService.getStats
    - `POST /report/generate`：调用 profileService.generateReport
    - `GET /report/latest`：调用 profileService.getLatestReport
    - 所有接口通过 StpUserUtil.getLoginIdAsInt() 获取当前用户 ID
    - 文件：`ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/controller/oj/AcProfileController.java`
    - _需求: 5.1-5.5_

- [x] 5. 后端检查点
  - 确保后端编译通过
  - 确认 `/ac/profile/**` 路径被现有 SecurityConfig 的 `/ac/**` 规则覆盖

- [x] 6. 前端 API 层和类型
  - [x] 6.1 在 types.ts 中新增类型
    - AcUserStatsVo, CategoryStat, AcAiReportVo
    - 文件：`alpencode-web/src/api/types.ts`
    - _需求: 2.1, 3.6_

  - [x] 6.2 创建 profile.ts API 文件
    - updateProfile, updatePassword, getStats, generateReport, getLatestReport
    - 文件：`alpencode-web/src/api/profile.ts`
    - _需求: 5.1-5.5_

  - [x] 6.3 安装 ECharts 依赖
    - 在 alpencode-web 目录执行 `npm install echarts`
    - _需求: 4.2-4.5_

- [x] 7. 前端个人中心页面
  - [x] 7.1 创建 ProfilePage（views/profile/index.vue）
    - 用户信息卡片（用户名、邮箱、注册时间）
    - 修改信息 Modal（复用 AcUser 类型的 username + email）
    - 修改密码 Modal（旧密码 + 新密码 + 确认密码，前端校验一致性）
    - 刷题概览数字展示
    - AI 报告入口（生成/查看/重新生成按钮，根据 status 切换状态）
    - 暗色主题风格
    - 文件：`alpencode-web/src/views/profile/index.vue`
    - _需求: 1.1-1.8, 3.3-3.6_

- [x] 8. 前端 AI 报告展示页面
  - [x] 8.1 创建 ReportPage（views/report/index.vue）
    - 全屏滚动：CSS scroll-snap-type: y mandatory，每屏 100vh
    - 第一屏：总览（注册天数、总提交、通过数、通过率 + AI 个性标签）
    - 第二屏：难度分布（ECharts 环形图 + AI 点评）
    - 第三屏：分类能力（ECharts 雷达图 + AI 点评）
    - 第四屏：错误分析（ECharts 饼图 + AI 分析）
    - 第五屏：综合评价（AI 文字 + 学习建议）
    - IntersectionObserver 淡入动画 + 返回按钮
    - 文件：`alpencode-web/src/views/report/index.vue`
    - _需求: 4.1-4.8_

- [x] 9. 路由和导航集成
  - [x] 9.1 新增 /profile 和 /report 路由
    - 文件：`alpencode-web/src/router/index.ts`

  - [x] 9.2 顶栏用户下拉菜单添加"个人中心"入口
    - 修改 main/index.vue 的用户下拉菜单，添加"个人中心"项
    - _需求: 1.1_

- [x] 10. 最终检查点
  - 前后端编译通过
  - 个人中心：信息展示、修改用户名/邮箱、修改密码
  - AI 报告：生成 → 轮询状态 → 查看全屏报告
  - 报告页面：全屏滚动、ECharts 图表、动画效果

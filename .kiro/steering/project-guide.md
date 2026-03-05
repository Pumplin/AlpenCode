---
inclusion: always
---

# AlpenCode 项目顶层指导文件

## 项目概述

AlpenCode 是一个 AI 辅助在线编程训练平台（毕设项目），核心理念是"让 AI 成为你的编程登山杖"。
系统集成 OJ 在线判题 + AI 错误诊断 + AI 代码优化建议 + AI 个性化能力报告。

## 系统架构

系统分为三端：
- 管理端前端：管理员使用，管理题库、测试用例、用户等数据
- 用户端前端：普通用户使用，答题、在线编程、判题、查看AI分析
- 后端：统一一套 Spring Boot 服务，同时为管理端和用户端提供 API

## 技术栈

### 后端（ruoyi-ai/）
- Java 17 + Spring Boot 3.4.4
- MyBatis-Plus 3.5.11（数据库访问）
- Sa-Token 1.34.0（认证鉴权）
- Spring AI（大模型调用，已配置 OpenAI 兼容接口 api.pandarobot.chat）
- RabbitMQ（判题任务队列，需新增依赖）
- Docker Java API（沙箱执行用户代码）
- MySQL 8.0（主数据库，库名 AlpenCode）
- Redis + Redisson（缓存 & 分布式锁）
- Hutool 5.8（工具库）
- Lombok + MapStruct-Plus

### 管理端前端（ruoyi-admin/apps/web-antd/）
- Vue 3 + TypeScript + Ant Design Vue + Vben Admin 框架
- 用于题目管理、测试用例管理、用户管理等后台功能

### 用户端前端（待搭建，独立项目或同项目新 app）
- Vue 3 + TypeScript + Ant Design Vue
- Monaco Editor（在线代码编辑器）
- 用于题库浏览、在线编程、判题、AI 分析结果展示

### 基础设施
- 服务端口：6039
- 数据库地址：117.78.1.49:3306/AlpenCode
- Redis 地址：117.78.1.49:6379
- RabbitMQ：需部署（建议同服务器）
- Docker：服务器需安装 Docker，用于代码沙箱执行

## 项目结构

```
AlpenCode/
├── ruoyi-ai/                          # 后端（Java Spring Boot）
│   ├── ruoyi-admin/                   # 启动模块（入口）
│   ├── ruoyi-common/                  # 通用模块（已有：core, redis, mybatis, satoken, oss, web 等）
│   ├── ruoyi-modules/                 # 业务模块
│   │   └── ruoyi-system/              # 系统模块（用户、角色、菜单等已有）
│   ├── ruoyi-modules-api/             # 模块间 API 接口
│   └── script/sql/                    # SQL 脚本
├── ruoyi-admin/                       # 管理端前端（Vue 3 + Ant Design + Vben Admin）
│   └── apps/web-antd/
│       └── src/
│           ├── api/                   # 接口定义
│           ├── views/                 # 页面（按模块分目录）
│           ├── router/                # 路由（后端动态菜单）
│           ├── store/                 # 状态管理
│           └── components/            # 公共组件
├── AlpenCode.sql                      # 完整数据库 SQL（ac_ 前缀为业务表）
└── .kiro/steering/                    # Kiro 指导文件
```

## 数据库设计（已确认）

业务表使用 `ac_` 前缀，主键为 int AUTO_INCREMENT，字段风格为 `created_at`/`updated_at`/`is_delete`。

### ac_problem（题目表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | int AI PK | 题目ID |
| title | varchar(200) | 题目标题 |
| description | text | 题目描述（Markdown） |
| difficulty | int | 难度（1=简单 2=中等 3=困难） |
| time_limit | int | 时间限制(ms)，默认1000 |
| memory_limit | int | 内存限制(MB)，默认256 |
| submit_count | int | 提交次数 |
| ac_count | int | 通过次数 |
| status | int | 状态 |
| is_delete | tinyint | 逻辑删除 |
| created_at / updated_at | datetime | 时间戳 |

### ac_problem_category（题目分类表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | int AI PK | 分类ID |
| name | varchar(50) UNIQUE | 分类名称（数组、字符串、DP等） |
| is_delete | tinyint | 逻辑删除 |
| created_at / updated_at | datetime | 时间戳 |

### ac_problem_category_map（题目-分类关联表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | int AI PK | 映射ID |
| problem_id | int | 题目ID |
| category_id | int | 分类ID |

### ac_test_case（测试用例表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | int AI PK | 用例ID |
| problem_id | int | 所属题目ID |
| input | text | 输入数据 |
| expected_output | text | 期望输出 |
| is_sample | tinyint | 是否公开样例（1=公开，用于Run Code） |
| sort | int | 排序 |
| status | int | 状态 |
| is_delete | tinyint | 逻辑删除 |
| created_at / updated_at | datetime | 时间戳 |

### ac_submit（提交记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | int AI PK | 提交ID |
| user_id | int | 用户ID（关联 ac_user） |
| problem_id | int | 题目ID |
| language | varchar(20) | 编程语言（java/python） |
| code | mediumtext | 用户提交的代码 |
| result | int | 判题结果（0=PENDING 1=JUDGING 2=AC 3=WA 4=TLE 5=MLE 6=RE 7=CE） |
| time_cost | int | 耗时(ms) |
| memory_cost | int | 内存(MB) |
| error_log | mediumtext | 错误日志/编译错误信息 |
| pass_count | int | 通过的测试用例数 |
| total_count | int | 总测试用例数 |
| is_delete | tinyint | 逻辑删除 |
| created_at / updated_at | datetime | 时间戳 |

### ac_user（用户表，独立于框架 sys_user）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | int AI PK | 用户ID |
| username | varchar(50) | 用户名 |
| password_hash | varchar(255) | 密码哈希 |
| email | varchar(100) | 邮箱 |
| role | enum('USER','ADMIN') | 角色 |
| is_delete | tinyint | 逻辑删除 |
| created_at / updated_at | datetime | 时间戳 |

> 注意：ac_user 是 OJ 业务的独立用户体系，管理端使用框架的 sys_user 登录，用户端使用 ac_user 登录。后端需要为两套用户体系分别实现认证逻辑。

## 核心功能模块（按优先级）

### P0 - 必须完成

#### 1. 题库管理模块
- 管理端：题目 CRUD、分类管理、测试用例管理
- 用户端：题库浏览、按分类/难度筛选
- 后端 API：统一在 `org.ruoyi.system.controller.oj` 下，通过权限区分管理端和用户端接口

#### 2. 在线编程模块（用户端）
- 集成 Monaco Editor 作为代码编辑器
- 支持语言：Java、Python
- 页面布局：左侧题目描述 + 右侧代码编辑器
- Run Code：运行公开样例（is_sample=1 的测试用例），快速调试
- Submit：提交到 RabbitMQ 判题队列，运行所有测试用例

#### 3. OJ 判题系统
- RabbitMQ 队列：`oj.judge.queue`
- 判题服务消费消息，使用 Docker 容器执行用户代码
- 流程：接收提交 → 创建 Docker 容器 → 编译运行 → 逐个测试用例对拍 → 更新 ac_submit 结果
- 判题结果值：0=PENDING 1=JUDGING 2=AC 3=WA 4=TLE 5=MLE 6=RE 7=CE
- Docker 沙箱要点：限制 CPU/内存/网络、设置执行超时、用完即销毁

#### 4. 提交记录模块（用户端）
- 用户查看自己的提交历史
- 展示：题目、语言、结果状态、耗时、内存、通过用例数/总用例数、提交时间

### P1 - 应该完成

#### 5. AI 错误诊断
- Submit 结果为 WA/RE/TLE/CE 时，自动调用 AI 分析
- 输入：用户代码 + 题目描述 + 错误状态 + error_log
- 输出：错误原因分析、修复思路
- AI 分析结果直接返回前端展示（不单独建表存储）

#### 6. AI 代码优化建议
- Submit 结果为 AC 时，自动调用 AI 分析
- 输入：用户代码 + 题目描述
- 输出：时间/空间复杂度分析、优化建议、更优解法提示


## 后端开发规范

### 包结构（在 ruoyi-modules/ruoyi-system 下新增）
```
org.ruoyi.system/
├── controller/oj/          # OJ 相关 Controller（管理端+用户端接口）
├── service/oj/             # OJ 业务 Service 接口
├── service/oj/impl/        # Service 实现
├── mapper/oj/              # MyBatis Mapper
├── domain/entity/          # 实体类（对应 ac_ 表）
├── domain/vo/              # VO（返回前端的数据）
├── domain/bo/              # BO（接收前端参数）
└── judge/                  # 判题核心逻辑（Docker 沙箱、编译执行、MQ 消费者）
```

### 实体类字段映射
ac_ 表使用 `created_at`/`updated_at`/`is_delete` 风格，实体类中需要用 `@TableField` 注解映射：
```java
@TableField("created_at")
private LocalDateTime createdAt;

@TableField("updated_at")
private LocalDateTime updatedAt;

@TableLogic
@TableField("is_delete")
private Integer isDelete;
```

### 代码规范
- Controller 层只做参数校验和调用 Service，不写业务逻辑
- Service 接口 + Impl 实现分离
- 使用 MyBatis-Plus 的 ServiceImpl 基类
- 统一返回 `R<T>` 响应体（框架已有）
- 管理端接口使用 `@SaCheckPermission` 做权限控制
- 用户端接口使用 ac_user 的认证体系

### AI 服务调用
- 使用 Spring AI 的 ChatClient
- 已配置 base-url: https://api.pandarobot.chat/
- AI 调用做异步处理，判题完成后异步调用 AI，结果通过接口返回前端

## 管理端前端开发规范（ruoyi-admin/apps/web-antd/）

### 新增目录
```
views/oj/
├── problem/                # 题目管理（CRUD）
├── category/               # 分类管理
├── testCase/               # 测试用例管理
└── user/                   # OJ 用户管理
```

### API 定义
```
api/oj/
├── problem/
│   ├── index.ts
│   └── model.d.ts
├── category/
│   ├── index.ts
│   └── model.d.ts
└── testCase/
    ├── index.ts
    └── model.d.ts
```

### 组件规范
- 表格使用 VxeTable（框架已集成）
- 表单使用 Vben 的 useVbenForm
- 弹窗使用 useVbenModal
- 遵循现有管理端页面的代码风格

## 用户端前端开发规范

### 页面结构
```
views/
├── home/                   # 首页/题库列表
├── problem/                # 题目详情 + 在线编程（Monaco Editor）
├── submission/             # 我的提交记录
├── report/                 # AI 能力报告
├── login/                  # 用户登录
└── register/               # 用户注册
```

### 关键组件
- Monaco Editor 封装为独立组件
- 在线编程页面布局：左侧题目描述（可折叠），右侧上方代码编辑器，右侧下方运行结果/AI分析

## 10 天开发计划

### Day 1-2：后端基础 + 题库 API
- 在 ruoyi-system 下创建 OJ 相关的 entity/mapper/service/controller
- 实现题目 CRUD、分类 CRUD、测试用例 CRUD 接口
- 实现 ac_user 注册/登录接口
- 添加 RabbitMQ 依赖和配置

### Day 3-4：管理端前端 + 用户端骨架
- 管理端：题目管理页面、分类管理、测试用例管理
- 搭建用户端前端项目骨架
- 用户端：登录/注册页、题库列表页
- 集成 Monaco Editor，实现在线编程页面布局

### Day 5-6：判题系统
- 实现 Docker 沙箱执行引擎（Java/Python 编译运行）
- 实现 RabbitMQ 生产者（提交时发消息）和消费者（判题服务）
- 实现 Run Code（运行公开样例）和 Submit（完整判题）
- 用户端对接判题结果展示

### Day 7-8：提交记录 + AI 功能
- 实现提交记录列表页
- 实现 AI 错误诊断（WA/RE/TLE/CE 时触发）
- 实现 AI 代码优化建议（AC 时触发）
- 用户端展示 AI 分析结果

### Day 9：AI 能力报告 + 联调
- 实现 AI 个性化能力报告
- 全流程联调测试
- 修复 bug

### Day 10：收尾
- 补充测试数据（题库至少 20 道题）
- UI 细节优化
- 准备演示环境

## AI Prompt 设计参考

### 错误诊断 Prompt
```
你是一个编程教学助手。用户提交了以下代码解决一道编程题，但结果不正确。
题目：{problemDescription}
用户代码：{userCode}
编程语言：{language}
判题结果：{judgeResult}
错误信息：{errorMessage}
请分析错误原因，给出具体的修复思路。用中文回答，语言简洁易懂。
```

### 代码优化 Prompt
```
你是一个编程教学助手。用户提交了以下代码并通过了所有测试用例。
题目：{problemDescription}
用户代码：{userCode}
编程语言：{language}
请分析代码的时间和空间复杂度，并给出优化建议。如果有更优的解法思路，请简要说明。用中文回答。
```

### 能力报告 Prompt
```
你是一个编程学习分析师。以下是用户的刷题统计数据：
总提交：{totalSubmissions} 次
通过题目：{solvedCount} 题
通过率：{acceptRate}%
各难度通过情况：简单 {easyCount}、中等 {mediumCount}、困难 {hardCount}
常见错误类型：{errorTypes}
擅长分类：{strongTags}
薄弱分类：{weakTags}
请生成一份个性化的能力分析报告，包含能力评价、弱点分析和学习建议。用中文回答。
```

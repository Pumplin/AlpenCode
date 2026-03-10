# 需求文档：ac_user 用户端认证体系

## 简介

为 AlpenCode 用户端实现独立于管理端（sys_user）的认证体系。用户端使用 ac_user 表进行注册、登录和鉴权，通过 Sa-Token 多用户体系机制实现两套认证逻辑的隔离。

## 术语表

- **用户端认证服务（AcAuthService）**：负责 ac_user 用户注册、登录、登出、Token 校验的后端服务
- **用户端（UserClient）**：AlpenCode 的普通用户前端应用（alpencode-web）
- **管理端（AdminClient）**：AlpenCode 的管理员后台应用（ruoyi-admin/apps/web-antd）
- **ac_user**：OJ 业务独立用户表，与框架 sys_user 表完全隔离
- **Sa-Token**：项目使用的认证鉴权框架（v1.34.0）
- **StpUserUtil**：用户端专用的 Sa-Token StpLogic 工具类，与管理端的 StpUtil 隔离
- **BCrypt**：密码哈希算法，用于安全存储用户密码

## 需求

### 需求 1：用户注册

**用户故事：** 作为一个新用户，我希望通过用户名和密码注册 AlpenCode 账号，以便使用在线编程和判题功能。

#### 验收标准

1. WHEN 用户端提交包含用户名和密码的注册请求，THE AcAuthService SHALL 使用 BCrypt 对密码进行哈希处理后存入 ac_user 表，并返回注册成功响应
2. WHEN 注册请求中包含邮箱字段，THE AcAuthService SHALL 将邮箱作为可选信息一并存入 ac_user 表
3. WHEN 注册请求中的用户名已存在于 ac_user 表中，THE AcAuthService SHALL 拒绝注册并返回"用户名已存在"错误信息
4. WHEN 注册请求中的用户名长度不在 3-50 个字符范围内，THE AcAuthService SHALL 拒绝注册并返回用户名长度校验错误信息
5. WHEN 注册请求中的密码长度不在 6-20 个字符范围内，THE AcAuthService SHALL 拒绝注册并返回密码长度校验错误信息
6. THE AcAuthService SHALL 要求前端对密码进行加密传输（非明文），后端接收后解密再进行 BCrypt 哈希存储

### 需求 2：用户登录

**用户故事：** 作为一个已注册用户，我希望通过用户名和密码登录 AlpenCode，以便访问在线编程和判题功能。

#### 验收标准

1. WHEN 用户端提交正确的用户名和密码，THE AcAuthService SHALL 通过 StpUserUtil 生成用户端专属 Token（有效期 7 天），并返回 Token 和用户基本信息
2. WHEN 用户端提交的用户名不存在于 ac_user 表中，THE AcAuthService SHALL 返回"用户名不存在"错误信息
3. WHEN 用户端提交的密码与 ac_user 表中存储的密码哈希不匹配，THE AcAuthService SHALL 返回"密码错误"错误信息
4. WHEN 用户端提交的用户名对应的账号状态为停用（1），THE AcAuthService SHALL 返回"账号已被停用"错误信息
5. THE AcAuthService SHALL 在登录成功后将用户 ID、用户名存入 Sa-Token Session，供后续接口获取当前登录用户信息
6. THE AcAuthService SHALL 将 Token 信息缓存到 Redis，参考若依框架现有的 Token 缓存实现

### 需求 3：用户登出

**用户故事：** 作为一个已登录用户，我希望能够主动登出，以保护账号安全。

#### 验收标准

1. WHEN 已登录用户发送登出请求，THE AcAuthService SHALL 通过 StpUserUtil 注销当前用户的 Token 并清除 Redis 中对应的缓存，返回登出成功响应
2. WHEN 未登录用户发送登出请求，THE AcAuthService SHALL 返回登出成功响应（幂等处理）

### 需求 4：获取当前登录用户信息

**用户故事：** 作为一个已登录用户，我希望能获取自己的用户信息，以便前端展示用户名等个人数据。

#### 验收标准

1. WHEN 已登录用户请求获取当前用户信息，THE AcAuthService SHALL 返回当前用户的 ID、用户名和邮箱信息（排除密码哈希）
2. WHEN 未登录用户请求获取当前用户信息，THE AcAuthService SHALL 返回未登录错误（HTTP 401）

### 需求 5：用户端与管理端认证隔离

**用户故事：** 作为系统架构师，我希望用户端（ac_user）和管理端（sys_user）的认证体系完全隔离，以避免两套用户体系的 Token 互相干扰。

#### 验收标准

1. THE AcAuthService SHALL 使用独立的 Sa-Token StpLogic（type="ac_user"），与管理端默认的 StpUtil（type="login"）隔离
2. WHEN 用户端 Token 用于访问管理端接口，THE 管理端认证拦截器 SHALL 拒绝该请求
3. WHEN 管理端 Token 用于访问用户端受保护接口，THE 用户端认证拦截器 SHALL 拒绝该请求
4. THE 用户端认证拦截器 SHALL 对 `/oj/auth/login` 和 `/oj/auth/register` 路径放行，对 `/oj/` 下其他受保护路径要求用户端 Token 有效
5. THE AcAuthService SHALL 使用独立的 Token 名称前缀或参数，确保用户端和管理端的 Token 在 Redis 中存储隔离

### 需求 6：接口路径规范

**用户故事：** 作为后端开发者，我希望用户端认证接口遵循统一的路径规范，以便前端对接和后续维护。

#### 验收标准

1. THE AcAuthService SHALL 将注册接口暴露在 `POST /oj/auth/register` 路径
2. THE AcAuthService SHALL 将登录接口暴露在 `POST /oj/auth/login` 路径
3. THE AcAuthService SHALL 将登出接口暴露在 `POST /oj/auth/logout` 路径
4. THE AcAuthService SHALL 将获取当前用户信息接口暴露在 `GET /oj/auth/info` 路径

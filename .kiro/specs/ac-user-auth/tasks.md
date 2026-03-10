# 实现计划：ac_user 用户端认证体系

## 概述

基于 Sa-Token 多账号体系机制，为 AlpenCode 用户端实现独立于管理端的认证体系。后端创建 `StpUserUtil`（type="ac_user"）工具类、认证 Service 和 Controller，配置拦截器放行规则；前端调整 API 路径从 `/oj/user/*` 到 `/oj/auth/*`，并适配注册页面邮箱可选逻辑。

## 任务

- [x] 1. 创建 StpUserUtil 工具类和修改 DTO
  - [x] 1.1 创建 StpUserUtil 用户端 Sa-Token 工具类
    - 在 `org.ruoyi.common.satoken.utils` 包下创建 `StpUserUtil.java`
    - 定义 `public static final String TYPE = "ac_user"`
    - 创建 `public static StpLogic stpLogic = new StpLogic(TYPE)`
    - 实现静态方法：`login(Object id)`, `logout()`, `checkLogin()`, `getLoginIdAsInt()`, `getTokenValue()`, `isLogin()`, `getTokenSession()`
    - _需求: 5.1, 5.5_

  - [x] 1.2 修改 AcUserRegisterDTO 邮箱字段为可选
    - 移除 `email` 字段上的 `@NotBlank(message = "邮箱不能为空")` 注解，保留 `@Email` 校验
    - 文件路径：`ruoyi-ai/ruoyi-modules-api/ruoyi-system-api/src/main/java/org/ruoyi/system/domain/dto/AcUserRegisterDTO.java`
    - _需求: 1.2_

  - [x] 1.3 创建 AcLoginVo 登录响应对象
    - 在 `org.ruoyi.system.domain.vo` 包下创建 `AcLoginVo.java`
    - 包含字段：`String token`、`AcUserVo user`
    - _需求: 2.1_

- [x] 2. 实现认证 Service 层
  - [x] 2.1 创建 IAcAuthService 接口
    - 在 `org.ruoyi.system.service.oj` 包下创建 `IAcAuthService.java`
    - 定义方法：`void register(AcUserRegisterDTO dto)`, `AcLoginVo login(AcUserLoginDTO dto)`, `void logout()`, `AcUserVo getLoginUserInfo()`
    - _需求: 6.1, 6.2, 6.3, 6.4_

  - [x] 2.2 创建 AcAuthServiceImpl 实现类
    - 在 `org.ruoyi.system.service.oj.impl` 包下创建 `AcAuthServiceImpl.java`
    - 注入 `AcUserMapper`（已有）
    - **register 方法**：
      - 校验用户名是否已存在（`selectCount` by username）
      - 对前端 Base64 编码的密码进行解码
      - 使用 `BCrypt.hashpw` 对密码进行哈希
      - 设置默认 `status=0`，插入 ac_user 表
    - **login 方法**：
      - 根据用户名查询 ac_user（含 status 校验）
      - 用户不存在 → 抛出 `ServiceException("用户名不存在")`
      - 账号停用（status=1）→ 抛出 `ServiceException("账号已被停用")`
      - `BCrypt.checkpw` 校验密码失败 → 抛出 `ServiceException("密码错误")`
      - 调用 `StpUserUtil.login(userId)`，将用户名写入 `StpUserUtil.getTokenSession()`
      - 构建 `AcLoginVo`（token + AcUserVo）返回
    - **logout 方法**：
      - try-catch 包裹 `StpUserUtil.logout()`，捕获 `NotLoginException` 实现幂等
    - **getLoginUserInfo 方法**：
      - 通过 `StpUserUtil.getLoginIdAsInt()` 获取当前用户 ID
      - 查询 ac_user 表，转换为 `AcUserVo` 返回
    - _需求: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 4.1_

  - [x] 2.3 编写属性测试：注册-查询用户信息往返一致性
    - **Property 1: 注册-查询用户信息往返一致性**
    - **验证: 需求 1.1, 1.2, 4.1**

  - [x] 2.4 编写属性测试：用户名唯一性约束
    - **Property 2: 用户名唯一性约束**
    - **验证: 需求 1.3**

  - [x] 2.5 编写属性测试：注册输入校验拒绝非法长度
    - **Property 3: 注册输入校验拒绝非法长度**
    - **验证: 需求 1.4, 1.5**

  - [x] 2.6 编写属性测试：登录-Token 有效性往返
    - **Property 4: 登录-Token 有效性往返**
    - **验证: 需求 2.1, 1.6, 2.5**

  - [x] 2.7 编写属性测试：不存在的用户名登录失败
    - **Property 5: 不存在的用户名登录失败**
    - **验证: 需求 2.2**

  - [x] 2.8 编写属性测试：错误密码登录失败
    - **Property 6: 错误密码登录失败**
    - **验证: 需求 2.3**

  - [x] 2.9 编写属性测试：停用账号登录失败
    - **Property 7: 停用账号登录失败**
    - **验证: 需求 2.4**

  - [x] 2.10 编写属性测试：登出使 Token 失效
    - **Property 8: 登出使 Token 失效**
    - **验证: 需求 3.1**

  - [x] 2.11 编写属性测试：跨体系 Token 隔离
    - **Property 9: 跨体系 Token 隔离**
    - **验证: 需求 5.2, 5.3**

- [x] 3. 创建 AcAuthController 控制器
  - [x] 3.1 创建 AcAuthController
    - 在 `org.ruoyi.system.controller.oj` 包下创建 `AcAuthController.java`
    - `@RestController` + `@RequestMapping("/oj/auth")`
    - 注入 `IAcAuthService`
    - `POST /register`：接收 `@Valid @RequestBody AcUserRegisterDTO`，调用 `authService.register(dto)`，返回 `R.ok("注册成功")`
    - `POST /login`：接收 `@Valid @RequestBody AcUserLoginDTO`，调用 `authService.login(dto)`，返回 `R.ok(loginVo)`
    - `POST /logout`：调用 `authService.logout()`，返回 `R.ok()`
    - `GET /info`：调用 `authService.getLoginUserInfo()`，返回 `R.ok(userVo)`
    - _需求: 6.1, 6.2, 6.3, 6.4_

- [x] 4. 检查点 - 后端核心逻辑完成
  - 确保所有测试通过，如有问题请向用户确认。

- [x] 5. 配置 SecurityConfig 拦截器
  - [x] 5.1 在 SecurityConfig 中追加用户端路由校验规则
    - 修改 `ruoyi-ai/ruoyi-common/ruoyi-common-security/src/main/java/org/ruoyi/common/security/config/SecurityConfig.java`
    - 在现有 `SaInterceptor` 的 handler 中，追加对 `/oj/**` 路径的处理逻辑：
      - 匹配 `/oj/**` 路径（排除 `/oj/auth/login` 和 `/oj/auth/register`）
      - 调用 `StpUserUtil.checkLogin()` 进行用户端 Token 校验
    - 在 `application.yml` 的 `security.excludes` 中添加 `/oj/auth/login` 和 `/oj/auth/register`
    - _需求: 5.4, 5.2, 5.3_

  - [x] 5.2 编写单元测试验证拦截器配置
    - 测试 `/oj/auth/login` 和 `/oj/auth/register` 无需 Token 可访问
    - 测试 `/oj/` 下其他路径无 Token 时返回 401
    - 测试用户端 Token 不能通过管理端校验
    - _需求: 5.2, 5.3, 5.4_

- [x] 6. 前端 API 层对接
  - [x] 6.1 修改 auth.ts 接口路径
    - 将 `alpencode-web/src/api/auth.ts` 中的接口路径从 `/oj/user/*` 改为 `/oj/auth/*`：
      - `login`: `/oj/user/login` → `/oj/auth/login`
      - `register`: `/oj/user/register` → `/oj/auth/register`
      - `getUserInfo`: `/oj/user/info` → `/oj/auth/info`
    - 新增 `logout` 函数：`POST /oj/auth/logout`
    - 修改 `register` 函数的 `email` 参数为可选（`email?: string`）
    - _需求: 6.1, 6.2, 6.3, 6.4, 1.2_

  - [x] 6.2 修改注册页面邮箱为可选
    - 修改 `alpencode-web/src/views/register/index.vue`
    - 移除注册表单中邮箱字段的必填校验（当前 `handleRegister` 中要求 `form.value.email` 非空）
    - 邮箱输入框保留但标记为可选
    - _需求: 1.2_

  - [x] 6.3 对接登出功能
    - 修改 `alpencode-web/src/store/user.ts` 的 `logout` 方法，调用后端 `/oj/auth/logout` 接口后再清除本地状态
    - 在 `alpencode-web/src/layouts/MainLayout.vue`（如有登出按钮）中调用 store 的 logout
    - _需求: 3.1_

- [x] 7. 最终检查点 - 全流程验证
  - 确保所有测试通过，如有问题请向用户确认。
  - 验证注册→登录→获取用户信息→登出完整流程
  - 验证用户端和管理端 Token 隔离

## 备注

- 标记 `*` 的任务为可选，可跳过以加快 MVP 进度
- 每个任务引用了具体的需求编号，确保可追溯性
- 属性测试验证设计文档中定义的正确性属性
- 已有文件（AcUser.java、AcUserLoginDTO.java、AcUserVo.java、store/user.ts、request.ts）无需重新创建，仅需修改或直接使用

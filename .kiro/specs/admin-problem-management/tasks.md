# 实现计划：管理端题目管理

## 概述

基于已完成的后端 API，实现管理端题目管理前端功能。按照"基础设施 → API 层 → 公共组件 → 页面组件 → 集成联调"的顺序递进开发，每一步都在前一步基础上构建，确保无孤立代码。

## 任务

- [x] 1. 字典配置与枚举注册
  - [x] 1.1 在后端数据库中插入难度字典数据
    - 在 `sys_dict_type` 表中插入字典类型 `ac_difficulty`（字典名称：OJ 题目难度）
    - 在 `sys_dict_data` 表中插入三条字典数据：`1`→简单（list_class: `success`）、`2`→中等（list_class: `warning`）、`3`→困难（list_class: `danger`）
    - 提供可执行的 SQL 脚本
    - _需求：1.4_
  - [x] 1.2 在 DictEnum 中新增 AC_DIFFICULTY 枚举值
    - 在 `ruoyi-admin/packages/@core/base/shared/src/constants/dict-enum.ts` 中新增 `AC_DIFFICULTY = 'ac_difficulty'`
    - _需求：1.4_

- [x] 2. 前端 API 层封装
  - [x] 2.1 创建题目 API 类型定义和接口函数
    - 创建 `api/oj/problem/model.d.ts`，定义 `Problem` 接口（含 categories、categoryIds 字段）
    - 创建 `api/oj/problem/index.ts`，导出 `problemPage`、`problemInfo`、`problemAdd`、`problemUpdate`（使用 `postWithMsg`）、`problemRemove` 函数
    - 遵循 `api/system/notice/index.ts` 的封装模式，使用 `requestClient` 和 `PageQuery` 类型
    - _需求：6.1, 6.2, 6.5, 6.6_
  - [x] 2.2 创建分类 API 类型定义和接口函数
    - 创建 `api/oj/category/model.d.ts`，定义 `ProblemCategory` 接口
    - 创建 `api/oj/category/index.ts`，导出 `categoryPage`、`categoryList`、`categoryInfo`、`categoryAdd`、`categoryUpdate`（使用 `postWithMsg`）、`categoryRemove` 函数
    - `categoryList` 调用 `/oj/category/list` 返回全量分类列表，供题目编辑弹窗和筛选下拉使用
    - _需求：6.1, 6.3, 6.5, 6.6_
  - [x] 2.3 创建测试用例 API 类型定义和接口函数
    - 创建 `api/oj/testCase/model.d.ts`，定义 `TestCase` 接口
    - 创建 `api/oj/testCase/index.ts`，导出 `testCasePage`、`testCaseInfo`、`testCaseAdd`、`testCaseUpdate`（使用 `postWithMsg`）、`testCaseRemove` 函数
    - _需求：6.1, 6.4, 6.5, 6.6_

- [x] 3. Markdown 编辑器公共组件
  - [x] 3.1 安装 md-editor-v3 依赖并创建 MarkdownEditor 组件
    - 在 `ruoyi-admin/apps/web-antd/` 目录下执行 `pnpm add md-editor-v3`
    - 创建 `components/markdown/MarkdownEditor.vue`，封装 `MdEditor`，支持 `v-model` 双向绑定和并排实时预览
    - 创建 `components/markdown/index.ts` 导出组件
    - _需求：2.6_

- [x] 4. 检查点 - 确保基础设施就绪
  - 确保字典数据已插入、DictEnum 已更新、API 层文件无类型错误、MarkdownEditor 组件可正常导入。如有问题请向用户确认。

- [x] 5. 分类管理页面（独立模块，无外部依赖）
  - [x] 5.1 创建分类管理的表格列和弹窗表单 schema
    - 创建 `views/oj/category/data.ts`，定义 `columns`（分类ID、分类名称、创建时间、操作列）和 `modalSchema`（分类名称，必填）
    - _需求：5.2, 5.5_
  - [x] 5.2 创建分类编辑弹窗组件
    - 创建 `views/oj/category/category-modal.vue`，使用 `useVbenModal` + `useVbenForm`
    - 支持新增模式（调用 `categoryAdd`）和编辑模式（调用 `categoryUpdate`，请求体含 id）
    - 提交成功后 emit `reload` 事件并关闭弹窗
    - _需求：5.5, 5.6, 5.7, 5.10_
  - [x] 5.3 创建分类管理列表页
    - 创建 `views/oj/category/index.vue`，使用 `useVbenVxeGrid` 展示分页表格
    - 工具栏提供"新增"按钮，每行提供"编辑"和"删除"操作
    - 删除操作使用 `Popconfirm` 确认气泡框，支持复选框批量删除
    - 调用 `categoryPage` 加载数据，调用 `categoryRemove` 删除
    - _需求：5.1, 5.2, 5.3, 5.4, 5.8, 5.9, 5.10_
  - [ ]* 5.4 编写分类管理属性测试
    - **属性 3：新增/编辑模式 API 路由正确性（分类部分）**
    - **验证需求：5.6, 5.7**

- [x] 6. 题目管理列表页与编辑弹窗
  - [x] 6.1 创建题目管理的表格列、筛选表单和弹窗表单 schema
    - 创建 `views/oj/problem/data.tsx`，定义：
      - `querySchema`：筛选表单（题目分类 Select 下拉、题目标题 Input、难度 Select 使用 `getDictOptions(DictEnum.AC_DIFFICULTY)`、状态 Select）
      - `columns`：表格列（题目ID、标题、难度使用 `renderDict` 渲染彩色标签、时间限制、内存限制、提交次数、通过次数、状态、创建时间、操作列）
    - 分类下拉数据通过 `categoryList()` 接口异步加载
    - _需求：1.1, 1.2, 1.4_
  - [x] 6.2 创建题目编辑弹窗组件
    - 创建 `views/oj/problem/problem-modal.vue`，参考 `notice-modal.vue` 使用原生 Form + `Form.useForm` 校验
    - 表单字段：题目标题（Input，必填）、题目描述（MarkdownEditor，必填）、难度（Select，使用 `getDictOptions(DictEnum.AC_DIFFICULTY)`，必填）、时间限制（InputNumber，默认 1000）、内存限制（InputNumber，默认 256）、状态（Select）、题目分类（Select 多选，数据来源 `categoryList()`）
    - 新增模式调用 `problemAdd`，编辑模式调用 `problemUpdate`（请求体含 id 和 categoryIds）
    - 编辑模式打开时调用 `problemInfo(id)` 预填所有字段
    - 提交成功后 emit `reload` 并关闭弹窗
    - _需求：2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  - [x] 6.3 创建题目管理列表页
    - 创建 `views/oj/problem/index.vue`，使用 `useVbenVxeGrid` + `useVbenModal`
    - 工具栏提供"新增"按钮，每行提供"编辑"、"详情"、"删除"操作
    - "详情"按钮通过 `router.push` 跳转到详情页，携带 problemId
    - 删除使用 `Popconfirm` 确认，支持复选框批量删除
    - 筛选条件变更后携带参数调用 `problemPage` 刷新表格
    - _需求：1.1, 1.2, 1.3, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_
  - [ ]* 6.4 编写题目管理属性测试
    - **属性 1：难度字典渲染正确性**
    - **属性 4：必填字段校验阻止提交**
    - **属性 6：筛选参数透传完整性**
    - **验证需求：1.3, 1.4, 2.7**

- [x] 7. 检查点 - 确保列表页和弹窗功能正常
  - 确保分类管理和题目管理的列表页、弹窗组件无类型错误，所有 API 调用路径正确。如有问题请向用户确认。

- [x] 8. 题目详情页与测试用例管理
  - [x] 8.1 创建测试用例的表格列和弹窗表单 schema
    - 创建 `views/oj/testCase/data.ts`，定义 `columns`（用例ID、输入数据、期望输出、是否公开样例、排序、状态、操作列）和 `modalSchema`（输入数据 Textarea 必填、期望输出 Textarea 必填、是否公开样例 Switch 默认否、排序 InputNumber 默认 0、状态 Select）
    - _需求：4.1, 4.5_
  - [x] 8.2 创建测试用例编辑弹窗组件
    - 创建 `views/oj/testCase/testCase-modal.vue`，使用 `useVbenModal` + `useVbenForm`
    - 新增模式自动绑定当前 `problemId`（通过 `modalApi.getData()` 传入），调用 `testCaseAdd`
    - 编辑模式调用 `testCaseUpdate`，请求体含测试用例 id
    - 提交成功后 emit `reload` 并关闭弹窗
    - _需求：4.5, 4.6, 4.7, 4.9_
  - [x] 8.3 创建题目详情页（含测试用例表格）
    - 创建 `views/oj/problem/detail.vue`
    - 页面上方以只读模式展示题目信息：标题、描述（使用 `MdPreview` 渲染 Markdown）、难度、时间限制、内存限制、提交次数、通过次数、状态、关联分类、创建时间
    - 页面下方嵌入测试用例 VxeTable 表格，以当前 `problemId` 作为筛选参数调用 `testCasePage`
    - 测试用例表格提供"新增用例"按钮，每行提供"编辑"和"删除"操作
    - 删除使用 `Popconfirm` 确认气泡框
    - 提供"返回"按钮，点击后 `router.back()` 返回列表页
    - _需求：3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 4.8, 4.9_
  - [ ]* 8.4 编写测试用例管理属性测试
    - **属性 9：测试用例绑定题目 ID**
    - **属性 5：操作成功后列表刷新**
    - **验证需求：4.2, 4.6, 4.9**

- [x] 9. 最终检查点 - 全部功能验证
  - 确保所有页面组件无类型错误，API 层类型定义与后端 VO 字段对齐，所有路由跳转和弹窗交互正常。如有问题请向用户确认。

## 备注

- 标记 `*` 的任务为可选任务，可跳过以加快 MVP 进度
- 每个任务引用了具体的需求编号，确保可追溯性
- 后端 API 已全部实现，所有任务聚焦于前端开发
- 检查点任务用于阶段性验证，确保增量开发的正确性
- 属性测试验证设计文档中定义的通用正确性属性

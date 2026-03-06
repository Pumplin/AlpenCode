# 需求文档

## 简介

AlpenCode 管理端的题目管理与测试用例管理前端功能。管理员通过该模块对 OJ 题库进行维护，包括题目的增删改查、题目分类管理，以及在题目详情页中管理测试用例。前端基于 Vue 3 + TypeScript + Ant Design Vue + Vben Admin 框架，遵循现有管理端页面的代码风格（VxeTable 表格、useVbenModal 弹窗、useVbenForm 表单）。

## 术语表

- **Problem_List_Page**: 题目管理列表页面，展示所有题目并提供筛选、新增、编辑、查看详情、删除操作
- **Problem_Edit_Modal**: 题目编辑弹窗，用于新增或修改题目信息
- **Problem_Detail_Page**: 题目详情页面，只读展示题目信息并管理该题目的测试用例
- **Category_List_Page**: 分类管理列表页面，独立菜单入口，管理题目分类的增删改查
- **Category_Modal**: 分类编辑弹窗，用于新增或修改分类
- **TestCase_Table**: 测试用例表格，嵌入在 Problem_Detail_Page 中，管理指定题目的测试用例
- **TestCase_Modal**: 测试用例编辑弹窗，用于新增或修改测试用例
- **Markdown_Editor**: Markdown 编辑器组件，用于编辑题目描述，支持实时预览
- **Admin_API_Layer**: 前端 API 层，封装对后端 `/oj/problem`、`/oj/category`、`/oj/testCase` 接口的调用
- **Difficulty_Dict**: 难度字典，映射值 1=简单、2=中等、3=困难

## 需求

### 需求 1：题目管理列表页

**用户故事：** 作为管理员，我希望在列表页中浏览、筛选和管理所有题目，以便我能高效地维护题库数据

#### 验收标准

1. 当管理员导航到题目管理菜单时，Problem_List_Page 应使用 VxeTable 展示分页题目表格，包含以下列：题目ID、题目标题、难度、时间限制(ms)、内存限制(MB)、提交次数、通过次数、状态、创建时间、操作
2. Problem_List_Page 应提供以下筛选字段：题目分类（Select 下拉，数据来源于分类列表接口）、题目标题（Input 模糊搜索）、难度（Select 下拉，使用 Difficulty_Dict）、状态（Select 下拉）
3. 当管理员修改任意筛选条件并触发搜索时，Problem_List_Page 应携带筛选参数调用后端 `/oj/problem/page` 接口并刷新表格数据
4. Problem_List_Page 应使用 Difficulty_Dict 以标签形式渲染难度列（简单=绿色，中等=橙色，困难=红色）
5. Problem_List_Page 应在工具栏区域提供"新增"按钮，点击后打开 Problem_Edit_Modal 用于创建新题目
6. Problem_List_Page 应为每行提供"编辑"和"详情"操作按钮
7. 当管理员点击某行的"编辑"按钮时，Problem_List_Page 应打开 Problem_Edit_Modal 并预填该题目的数据
8. 当管理员点击某行的"详情"按钮时，Problem_List_Page 应导航到该题目的 Problem_Detail_Page
9. Problem_List_Page 应支持复选框选择并提供批量"删除"按钮，调用后端 `DELETE /oj/problem/{ids}` 接口
10. 当触发单行"删除"操作时，Problem_List_Page 应在调用删除接口前显示确认气泡框


### 需求 2：题目编辑弹窗

**用户故事：** 作为管理员，我希望通过弹窗表单新增或编辑题目，以便我能方便地维护题目内容

#### 验收标准

1. Problem_Edit_Modal 应包含以下表单字段：题目标题（Input，必填）、题目描述（Markdown_Editor，必填）、难度（Select，使用 Difficulty_Dict，必填）、时间限制（InputNumber，单位 ms，默认 1000）、内存限制（InputNumber，单位 MB，默认 256）、状态（Select）、题目分类（Select 多选，数据来源于分类列表接口）
2. 当管理员打开弹窗编辑已有题目时，Problem_Edit_Modal 应调用后端 `GET /oj/problem/{id}` 接口并将返回数据预填到所有表单字段中
3. 当管理员提交新增题目表单时，Problem_Edit_Modal 应调用后端 `POST /oj/problem` 接口，提交表单数据（包含 categoryIds）
4. 当管理员提交编辑已有题目表单时，Problem_Edit_Modal 应调用后端 `POST /oj/problem/edit` 接口，提交表单数据（包含题目 id 和 categoryIds）
5. 当后端返回提交成功响应后，Problem_Edit_Modal 应关闭弹窗并触发 Problem_List_Page 的表格刷新
6. Markdown_Editor 应支持编辑区域与 Markdown 内容的并排实时预览
7. 如果管理员提交表单时存在未填写的必填字段，Problem_Edit_Modal 应在对应字段上显示校验错误信息并阻止提交

### 需求 3：题目详情页

**用户故事：** 作为管理员，我希望查看题目的完整信息并在同一页面管理测试用例，以便我能在一个上下文中完成题目和用例的维护

#### 验收标准

1. 当管理员导航到 Problem_Detail_Page 时，该页面应调用后端 `GET /oj/problem/{id}` 接口并以只读模式展示题目信息：题目标题、题目描述（Markdown 渲染）、难度、时间限制、内存限制、提交次数、通过次数、状态、关联分类、创建时间
2. Problem_Detail_Page 应将题目描述字段渲染为格式化的 Markdown 内容
3. Problem_Detail_Page 应在题目信息区域下方展示 TestCase_Table
4. Problem_Detail_Page 应提供"返回"按钮，点击后导航回 Problem_List_Page

### 需求 4：测试用例管理

**用户故事：** 作为管理员，我希望在题目详情页中管理测试用例，以便我能为每道题目配置输入输出数据

#### 验收标准

1. TestCase_Table 应使用 VxeTable 展示当前题目的分页测试用例表格，包含以下列：用例ID、输入数据、期望输出、是否公开样例、排序、状态、操作
2. 当 Problem_Detail_Page 加载时，TestCase_Table 应以当前 problemId 作为筛选参数调用后端 `GET /oj/testCase/page` 接口
3. TestCase_Table 应提供"新增用例"按钮，点击后打开 TestCase_Modal 用于创建绑定到当前题目的新测试用例
4. 当管理员点击测试用例行的"编辑"按钮时，TestCase_Table 应打开 TestCase_Modal 并预填该测试用例的数据
5. TestCase_Modal 应包含以下表单字段：输入数据（Textarea，必填）、期望输出（Textarea，必填）、是否公开样例（Switch，默认否）、排序（InputNumber，默认 0）、状态（Select）
6. 当管理员提交新增测试用例表单时，TestCase_Modal 应调用后端 `POST /oj/testCase` 接口，提交表单数据（包含当前 problemId）
7. 当管理员提交编辑已有测试用例表单时，TestCase_Modal 应调用后端 `POST /oj/testCase/edit` 接口，提交表单数据（包含测试用例 id）
8. 当管理员点击测试用例行的"删除"按钮时，TestCase_Table 应显示确认气泡框，确认后调用后端 `DELETE /oj/testCase/{ids}` 接口
9. 当任意测试用例增删改操作成功后，TestCase_Table 应刷新表格数据


### 需求 5：分类管理页面

**用户故事：** 作为管理员，我希望在独立页面中管理题目分类，以便我能维护分类标签供题目关联使用

#### 验收标准

1. Category_List_Page 应作为管理端导航中的独立菜单入口可访问
2. Category_List_Page 应使用 VxeTable 展示分页分类表格，包含以下列：分类ID、分类名称、创建时间、操作
3. 当 Category_List_Page 加载时，该页面应调用后端 `GET /oj/category/page` 接口并填充表格数据
4. Category_List_Page 应提供"新增"按钮，点击后打开 Category_Modal 用于创建新分类
5. Category_Modal 应包含表单字段：分类名称（Input，必填）
6. 当管理员提交新增分类表单时，Category_Modal 应调用后端 `POST /oj/category` 接口
7. 当管理员提交编辑已有分类表单时，Category_Modal 应调用后端 `POST /oj/category/edit` 接口
8. 当管理员点击分类行的"删除"按钮时，Category_List_Page 应显示确认气泡框，确认后调用后端 `DELETE /oj/category/{ids}` 接口
9. Category_List_Page 应支持复选框选择并提供批量"删除"按钮
10. 当任意分类增删改操作成功后，Category_List_Page 应刷新表格数据

### 需求 6：前端 API 层

**用户故事：** 作为开发者，我希望前端 API 层按照项目规范封装所有后端接口调用，以便页面组件能统一、简洁地调用后端服务

#### 验收标准

1. Admin_API_Layer 应在 `api/oj/problem/`、`api/oj/category/`、`api/oj/testCase/` 目录下的独立 `model.d.ts` 文件中定义 Problem、ProblemCategory 和 TestCase 的 TypeScript 接口
2. Admin_API_Layer 应在 `api/oj/problem/index.ts` 中导出题目相关 API 函数：`problemPage`（分页查询）、`problemInfo`（详情）、`problemAdd`（新增）、`problemUpdate`（修改）、`problemRemove`（删除）
3. Admin_API_Layer 应在 `api/oj/category/index.ts` 中导出分类相关 API 函数：`categoryPage`（分页查询）、`categoryList`（全量列表）、`categoryInfo`（详情）、`categoryAdd`（新增）、`categoryUpdate`（修改）、`categoryRemove`（删除）
4. Admin_API_Layer 应在 `api/oj/testCase/index.ts` 中导出测试用例相关 API 函数：`testCasePage`（分页查询）、`testCaseInfo`（详情）、`testCaseAdd`（新增）、`testCaseUpdate`（修改）、`testCaseRemove`（删除）
5. Admin_API_Layer 应使用 `#/api/request` 中已有的 `requestClient`，并遵循现有 API 模块的相同模式（如 `api/system/notice/index.ts`）
6. Admin_API_Layer 应使用 `#/api/common` 中的 `PageQuery` 类型作为分页请求参数类型
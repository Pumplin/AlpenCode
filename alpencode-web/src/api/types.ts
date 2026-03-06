/** 通用分页请求参数 */
export interface PageParams {
  pageNum?: number;
  pageSize?: number;
}

/** 通用分页响应 */
export interface PageResult<T> {
  rows: T[];
  total: number;
}

/** 通用响应体 */
export interface R<T = any> {
  code: number;
  msg: string;
  data: T;
}

/** 题目 */
export interface Problem {
  id: number;
  title: string;
  description: string;
  difficulty: number;
  timeLimit: number;
  memoryLimit: number;
  submitCount: number;
  acCount: number;
  status: number;
  categories?: ProblemCategory[];
  createdAt: string;
}

/** 题目分类 */
export interface ProblemCategory {
  id: number;
  name: string;
}

/** 测试用例 */
export interface TestCase {
  id: number;
  problemId: number;
  input: string;
  expectedOutput: string;
  isSample: number;
  sort: number;
}

/** 提交记录 */
export interface Submit {
  id: number;
  userId: number;
  problemId: number;
  language: string;
  code: string;
  result: number;
  timeCost: number;
  memoryCost: number;
  errorLog: string;
  passCount: number;
  totalCount: number;
  createdAt: string;
  /** 关联的题目标题（列表展示用） */
  problemTitle?: string;
}

/** 用户 */
export interface AcUser {
  id: number;
  username: string;
  email: string;
  role: string;
}

/** 登录请求 */
export interface LoginParams {
  username: string;
  password: string;
}

/** 登录响应 */
export interface LoginResult {
  token: string;
  user: AcUser;
}

/** 代码运行请求 */
export interface RunCodeParams {
  problemId: number;
  language: string;
  code: string;
}

/** 代码运行结果 */
export interface RunCodeResult {
  passed: boolean;
  output: string;
  expectedOutput: string;
  timeCost: number;
  memoryCost: number;
  errorLog?: string;
}

/** 提交代码请求 */
export interface SubmitCodeParams {
  problemId: number;
  language: string;
  code: string;
}

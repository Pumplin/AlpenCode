import request from './request';
import type { RunCodeParams, RunCodeResult, SubmitCodeParams, R } from './types';

/** Run Code — 运行公开样例 */
export function runCode(data: RunCodeParams) {
  return request.post<any, R<RunCodeResult[]>>('/oj/judge/run', data);
}

/** Submit — 提交判题 */
export function submitCode(data: SubmitCodeParams) {
  return request.post<any, R<{ submitId: number }>>('/oj/judge/submit', data);
}

/** 查询提交结果（轮询用） */
export function getSubmitResult(submitId: number) {
  return request.get<any, R<import('./types').Submit>>(`/oj/submit/info/${submitId}`);
}

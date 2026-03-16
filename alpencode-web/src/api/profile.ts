import request from './request';
import type { R, AcUserStatsVo, AcAiReportVo } from './types';

/** 修改用户信息 */
export function updateProfile(data: { username: string; email?: string }) {
  return request.put<any, R<void>>('/ac/profile/info', data);
}

/** 修改密码 */
export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<any, R<void>>('/ac/profile/password', data);
}

/** 获取刷题统计 */
export function getStats() {
  return request.get<any, R<AcUserStatsVo>>('/ac/profile/stats');
}

/** 触发生成 AI 报告 */
export function generateReport() {
  return request.post<any, R<number>>('/ac/profile/report/generate');
}

/** 查询最新报告 */
export function getLatestReport() {
  return request.get<any, R<AcAiReportVo>>('/ac/profile/report/latest');
}

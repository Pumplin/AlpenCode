import request from './request';
import type { PageParams, PageResult, Problem, ProblemCategory, R } from './types';

/** 题库列表（用户端，分页） */
export function getProblemList(params: PageParams & { categoryId?: number; difficulty?: number; title?: string }) {
  return request.get<any, R<PageResult<Problem>>>('/oj/problem/page', { params });
}

/** 题目详情 */
export function getProblemDetail(id: number) {
  return request.get<any, R<Problem>>(`/oj/problem/info/${id}`);
}

/** 分类列表 */
export function getCategoryList() {
  return request.get<any, R<ProblemCategory[]>>('/oj/category/list');
}

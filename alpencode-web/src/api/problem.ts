import request from './request';
import type { PageParams, PageResult, Problem, ProblemCategory, R } from './types';

/** 题库列表（轻量，仅 id/title/difficulty/submitCount/acCount，分页） */
export function getProblemSimpleList(params: PageParams & { categoryId?: number; difficulty?: number; title?: string }) {
  return request.get<any, R<PageResult<Problem>>>('/ac/problem/simple', { params });
}

/** 题库列表（用户端，分页，完整字段） */
export function getProblemList(params: PageParams & { categoryId?: number; difficulty?: number; title?: string }) {
  return request.get<any, R<PageResult<Problem>>>('/ac/problem/page', { params });
}

/** 题目详情 */
export function getProblemDetail(id: number) {
  return request.get<any, R<Problem>>(`/ac/problem/info/${id}`);
}

/** 分类列表 */
export function getCategoryList() {
  return request.get<any, R<ProblemCategory[]>>('/ac/problem/category/list');
}

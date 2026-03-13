import request from './request';
import type { PageParams, PageResult, Submit, R } from './types';

/** 我的提交记录（分页） */
export function getMySubmissions(params: PageParams & { problemId?: number; result?: number }) {
  return request.get<any, R<PageResult<Submit>>>('/ac/submit/page', { params });
}

/** 提交详情 */
export function getSubmitDetail(id: number) {
  return request.get<any, R<Submit>>(`/ac/submit/info/${id}`);
}

import type { Problem } from './model';

import type { ID, IDS, PageQuery } from '#/api/common';

import { requestClient } from '#/api/request';

enum Api {
  page = '/oj/problem/page',
  root = '/oj/problem',
}

/**
 * 题目分页查询
 * @param params 分页参数
 * @returns 分页结果
 */
export function problemPage(params?: PageQuery) {
  return requestClient.get<Problem[]>(Api.page, { params });
}

/**
 * 题目详情
 * @param id 题目ID
 * @returns 详情
 */
export function problemInfo(id: ID) {
  return requestClient.get<Problem>(`${Api.root}/${id}`);
}

/**
 * 题目新增
 * @param data 参数
 */
export function problemAdd(data: Partial<Problem>) {
  return requestClient.postWithMsg<void>(Api.root, data);
}

/**
 * 题目修改
 * @param data 参数
 */
export function problemUpdate(data: Partial<Problem>) {
  return requestClient.postWithMsg<void>(`${Api.root}/edit`, data);
}

/**
 * 题目删除
 * @param ids 题目ID列表
 */
export function problemRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

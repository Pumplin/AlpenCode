import type { ProblemCategory } from './model';

import type { ID, IDS, PageQuery } from '#/api/common';

import { requestClient } from '#/api/request';

enum Api {
  list = '/oj/category/list',
  page = '/oj/category/page',
  root = '/oj/category',
}

/**
 * 分类分页查询
 * @param params 分页参数
 * @returns 分页结果
 */
export function categoryPage(params?: PageQuery) {
  return requestClient.get<ProblemCategory[]>(Api.page, { params });
}

/**
 * 全量分类列表（供下拉选择使用）
 * @returns 分类列表
 */
export function categoryList() {
  return requestClient.get<ProblemCategory[]>(Api.list);
}

/**
 * 分类详情
 * @param id 分类ID
 * @returns 详情
 */
export function categoryInfo(id: ID) {
  return requestClient.get<ProblemCategory>(`${Api.root}/${id}`);
}

/**
 * 分类新增
 * @param data 参数
 */
export function categoryAdd(data: Partial<ProblemCategory>) {
  return requestClient.postWithMsg<void>(Api.root, data);
}

/**
 * 分类修改
 * @param data 参数
 */
export function categoryUpdate(data: Partial<ProblemCategory>) {
  return requestClient.postWithMsg<void>(`${Api.root}/edit`, data);
}

/**
 * 分类删除
 * @param ids 分类ID列表
 */
export function categoryRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

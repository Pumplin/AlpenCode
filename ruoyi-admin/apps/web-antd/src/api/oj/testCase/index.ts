import type { TestCase } from './model';

import type { ID, IDS, PageQuery } from '#/api/common';

import { requestClient } from '#/api/request';

enum Api {
  page = '/oj/testCase/page',
  root = '/oj/testCase',
}

/**
 * 测试用例分页查询
 * @param params 分页参数
 * @returns 分页结果
 */
export function testCasePage(params?: PageQuery) {
  return requestClient.get<TestCase[]>(Api.page, { params });
}

/**
 * 测试用例详情
 * @param id 测试用例ID
 * @returns 详情
 */
export function testCaseInfo(id: ID) {
  return requestClient.get<TestCase>(`${Api.root}/${id}`);
}

/**
 * 测试用例新增
 * @param data 参数
 */
export function testCaseAdd(data: Partial<TestCase>) {
  return requestClient.postWithMsg<void>(Api.root, data);
}

/**
 * 测试用例修改
 * @param data 参数
 */
export function testCaseUpdate(data: Partial<TestCase>) {
  return requestClient.postWithMsg<void>(`${Api.root}/edit`, data);
}

/**
 * 测试用例删除
 * @param ids 测试用例ID列表
 */
export function testCaseRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

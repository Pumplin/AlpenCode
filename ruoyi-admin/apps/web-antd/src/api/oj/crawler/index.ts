import type { CrawlerRequestBO, CrawlerResultVO } from './model';

import { requestClient } from '#/api/request';

enum Api {
  execute = '/oj/crawler/execute',
}

/**
 * 执行爬取操作
 * @param params 爬取请求参数
 * @returns 爬取结果
 */
export function executeCrawler(params: CrawlerRequestBO) {
  return requestClient.postWithMsg<CrawlerResultVO>(Api.execute, params);
}

/**
 * 爬取请求参数
 */
export interface CrawlerRequestBO {
  /**
   * 爬取题目数量
   */
  limit: number;
}

/**
 * 失败题目信息
 */
export interface FailedProblemVO {
  /**
   * 题目标题
   */
  title: string;

  /**
   * 失败原因
   */
  reason: string;
}

/**
 * 爬取结果
 */
export interface CrawlerResultVO {
  /**
   * 成功导入数量
   */
  successCount: number;

  /**
   * 跳过数量（重复）
   */
  skipCount: number;

  /**
   * 失败数量
   */
  failCount: number;

  /**
   * 失败题目列表
   */
  failedProblems: FailedProblemVO[];
}

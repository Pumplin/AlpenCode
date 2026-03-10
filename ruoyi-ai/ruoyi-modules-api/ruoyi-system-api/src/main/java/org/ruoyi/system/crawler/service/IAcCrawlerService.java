package org.ruoyi.system.crawler.service;

import org.ruoyi.system.crawler.domain.vo.CrawlerResultVO;

/**
 * LeetCode 爬虫服务接口
 * 
 * @author AlpenCode
 */
public interface IAcCrawlerService {
    
    /**
     * 执行爬取操作
     * 
     * @param limit 爬取题目数量
     * @return 爬取结果
     */
    CrawlerResultVO executeCrawl(Integer limit);
}

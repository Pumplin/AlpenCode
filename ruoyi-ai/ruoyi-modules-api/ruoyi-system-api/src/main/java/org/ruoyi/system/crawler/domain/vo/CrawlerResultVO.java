package org.ruoyi.system.crawler.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 爬虫执行结果 VO
 *
 * @author AlpenCode
 */
@Data
public class CrawlerResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功导入数量
     */
    private Integer successCount;

    /**
     * 跳过数量（重复）
     */
    private Integer skipCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 失败题目列表
     */
    private List<FailedProblemVO> failedProblems;
}

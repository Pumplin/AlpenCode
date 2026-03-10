package org.ruoyi.system.crawler.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * LeetCode 题目数据传输对象
 * 用于从 LeetCode API 获取题目数据并传输到 AlpenCode 平台
 *
 * @author AlpenCode
 */
@Data
public class LeetCodeProblemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 题目唯一标识（titleSlug）
     * 用于查询题目详情的 URL 友好标识符
     */
    private String titleSlug;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目描述（HTML 格式）
     */
    private String description;

    /**
     * 难度（Easy/Medium/Hard）
     */
    private String difficulty;

    /**
     * 分类标签列表
     */
    private List<String> tags;

    /**
     * 示例测试用例列表
     */
    private List<TestCaseDTO> exampleTestCases;

    /**
     * 各语言代码模板（JSON 字符串）
     */
    private String codeSnippets;

    /**
     * 函数签名元数据（JSON 字符串）
     */
    private String metaData;
}

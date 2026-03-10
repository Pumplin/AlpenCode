package org.ruoyi.system.crawler.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 测试用例数据传输对象
 * 用于表示从 LeetCode 题目描述中解析出的示例测试用例
 *
 * @author AlpenCode
 */
@Data
public class TestCaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 输入数据
     */
    private String input;

    /**
     * 期望输出
     */
    private String expectedOutput;
}

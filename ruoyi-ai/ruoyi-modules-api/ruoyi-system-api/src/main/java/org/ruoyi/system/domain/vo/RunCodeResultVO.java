package org.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * Run Code 单个用例结果
 * @author 32846
 */
@Data
public class RunCodeResultVO {

    /** 是否通过 */
    private Boolean passed;

    /** 实际输出 */
    private String output;

    /** 期望输出 */
    private String expectedOutput;

    /** 输入（公开样例展示用） */
    private String input;

    /** 耗时(ms) */
    private Integer timeCost;

    /** 错误信息（CE/RE时有值） */
    private String errorLog;
}

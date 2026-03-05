package org.ruoyi.system.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 提交记录查询条件DTO
 * @author 32846
 */
@Data
public class AcSubmitQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Integer userId;

    /** 题目ID */
    private Integer problemId;

    /** 编程语言 */
    private String language;

    /** 判题结果 */
    private Integer result;
}

package org.ruoyi.system.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 测试用例查询条件DTO
 * @author 32846
 */
@Data
public class AcTestCaseQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属题目ID */
    private Integer problemId;

    /** 是否公开样例 */
    private Integer isSample;

    /** 状态 */
    private Integer status;
}

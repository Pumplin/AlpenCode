package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 测试用例新增/修改DTO
 * @author 32846
 */
@Data
public class AcTestCaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用例ID（修改时必传） */
    private Integer id;

    @NotNull(message = "题目ID不能为空")
    private Integer problemId;

    private String input;

    private String expectedOutput;

    private Integer isSample;

    private Integer sort;

    private Integer status;
}

package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 提交记录新增/修改DTO
 * @author 32846
 */
@Data
public class AcSubmitDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 提交ID（修改时必传） */
    private Integer id;

    private Integer userId;

    @NotNull(message = "题目ID不能为空")
    private Integer problemId;

    @NotBlank(message = "编程语言不能为空")
    private String language;

    @NotBlank(message = "代码不能为空")
    private String code;

    private Integer result;
    private Integer timeCost;
    private Integer memoryCost;
    private String errorLog;
    private Integer passCount;
    private Integer totalCount;
}

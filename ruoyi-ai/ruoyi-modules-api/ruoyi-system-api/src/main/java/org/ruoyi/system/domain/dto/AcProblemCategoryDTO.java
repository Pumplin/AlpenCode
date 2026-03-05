package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题目分类新增/修改DTO
 * @author 32846
 */
@Data
public class AcProblemCategoryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类ID（修改时必传） */
    private Integer id;

    @NotBlank(message = "分类名称不能为空")
    private String name;
}

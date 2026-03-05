package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 题目新增/修改DTO
 * @author 32846
 */
@Data
public class AcProblemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题目ID（修改时必传） */
    private Integer id;

    private String title;

    private String description;

    private Integer difficulty;

    private Integer timeLimit;

    private Integer memoryLimit;

    private Integer status;

    /** 关联的分类ID列表 */
    private List<Integer> categoryIds;
}

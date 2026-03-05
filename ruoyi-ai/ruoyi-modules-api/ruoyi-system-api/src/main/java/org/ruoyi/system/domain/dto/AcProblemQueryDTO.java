package org.ruoyi.system.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题目查询条件DTO
 * @author 32846
 */
@Data
public class AcProblemQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题目标题（模糊查询） */
    private String title;

    /** 难度（1=简单 2=中等 3=困难） */
    private Integer difficulty;

    /** 状态 */
    private Integer status;

    /** 分类ID（按分类筛选） */
    private Integer categoryId;
}

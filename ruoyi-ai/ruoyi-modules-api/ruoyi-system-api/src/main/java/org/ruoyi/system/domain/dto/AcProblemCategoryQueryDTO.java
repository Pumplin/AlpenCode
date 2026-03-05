package org.ruoyi.system.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题目分类查询条件DTO
 * @author 32846
 */
@Data
public class AcProblemCategoryQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类名称（模糊查询） */
    private String name;
}

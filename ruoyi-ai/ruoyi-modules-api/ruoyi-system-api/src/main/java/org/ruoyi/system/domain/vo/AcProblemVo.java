package org.ruoyi.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.AcProblem;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目视图对象 ac_problem
 * @author 32846
 */
@Data
public class AcProblemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String title;
    private String description;
    private Integer difficulty;
    private Integer timeLimit;
    private Integer memoryLimit;
    private Integer submitCount;
    private Integer acCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联的分类列表（非数据库字段） */
    private List<AcProblemCategoryVo> categories;
}

package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题目-分类关联表 ac_problem_category_map
 * @author 32846
 */
@Data
@TableName("ac_problem_category_map")
public class AcProblemCategoryMap implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 映射ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 题目ID
     */
    private Integer problemId;

    /**
     * 分类ID
     */
    private Integer categoryId;
}

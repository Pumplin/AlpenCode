package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目表 ac_problem
 * @author 32846
 */
@Data
@TableName("ac_problem")
public class AcProblem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 题目ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目描述
     */
    private String description;
    /**
     * 各语言代码模板（JSON）
     */
    private String codeSnippets;

    /**
     * 函数签名元数据（JSON）
     */
    private String metaData;

    /**
     * 难度（1=简单 2=中等 3=困难）
     */
    private Integer difficulty;

    /**
     * 时间限制(ms)
     */
    private Integer timeLimit;

    /**
     * 内存限制(MB)
     */
    private Integer memoryLimit;

    /**
     * 提交次数
     */
    private Integer submitCount;

    /**
     * 通过次数
     */
    private Integer acCount;

    /**
     * 逻辑删除（0=存在 2=删除）
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 状态（0=正常 1=停用）
     */
    private Integer status;
}

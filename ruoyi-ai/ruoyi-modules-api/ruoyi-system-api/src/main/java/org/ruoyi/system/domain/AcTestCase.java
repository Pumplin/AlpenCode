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
 * 测试用例表 ac_test_case
 * @author 32846
 */
@Data
@TableName("ac_test_case")
public class AcTestCase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用例ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 所属题目ID
     */
    private Integer problemId;

    /**
     * 输入数据
     */
    private String input;

    /**
     * 期望输出
     */
    private String expectedOutput;

    /**
     * 是否公开样例（0=隐藏 1=公开，用于Run Code）
     */
    private Integer isSample;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 逻辑删除（0=存在 1=删除）
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
     * 状态
     */
    private Integer status;
}

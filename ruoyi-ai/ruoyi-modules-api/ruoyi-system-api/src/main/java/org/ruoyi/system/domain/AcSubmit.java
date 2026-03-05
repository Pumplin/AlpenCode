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
 * 提交记录表 ac_submit
 * @author 32846
 */
@Data
@TableName("ac_submit")
public class AcSubmit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提交ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 题目ID
     */
    private Integer problemId;

    /**
     * 编程语言（java/python）
     */
    private String language;

    /**
     * 用户提交的代码
     */
    private String code;

    /**
     * 判题结果（0=PENDING 1=JUDGING 2=AC 3=WA 4=TLE 5=MLE 6=RE 7=CE）
     */
    private Integer result;

    /**
     * 耗时(ms)
     */
    private Integer timeCost;

    /**
     * 内存(MB)
     */
    private Integer memoryCost;

    /**
     * 错误日志/编译错误信息
     */
    private String errorLog;

    /**
     * 通过的测试用例数
     */
    private Integer passCount;

    /**
     * 总测试用例数
     */
    private Integer totalCount;

    /**
     * 逻辑删除（0=存在 2=删除）
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 提交时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

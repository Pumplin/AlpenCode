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
 * AI能力报告表 ac_ai_report
 * @author 32846
 */
@Data
@TableName("ac_ai_report")
public class AcAiReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 报告ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 状态（0=生成中 1=已完成 2=失败）
     */
    private Integer status;

    /**
     * 统计数据快照（JSON）
     */
    private String statsSnapshot;

    /**
     * AI生成的报告内容（JSON）
     */
    private String reportContent;

    /**
     * 逻辑删除（0=存在 2=删除）
     */
    @TableLogic
    @TableField("is_delete")
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
}

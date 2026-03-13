package org.ruoyi.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.AcSubmit;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交记录视图对象 ac_submit
 * @author 32846
 */
@Data
public class AcSubmitVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer userId;
    private Integer problemId;
    private String language;
    private String code;
    private Integer result;
    private Integer timeCost;
    private Integer memoryCost;
    private String errorLog;
    /** 每个测试用例的详细判题结果（JSON 数组） */
    private String judgeDetails;
    /** AI 分析结果 */
    private String aiAnalysis;
    private Integer passCount;
    private Integer totalCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 题目标题（非数据库字段，列表展示用） */
    private String problemTitle;
}

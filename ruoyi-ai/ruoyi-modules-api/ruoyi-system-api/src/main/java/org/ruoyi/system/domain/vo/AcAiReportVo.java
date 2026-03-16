package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 能力报告视图 VO
 * @author 32846
 */
@Data
public class AcAiReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报告ID */
    private Integer id;

    /** 状态（0=生成中 1=已完成 2=失败） */
    private Integer status;

    /** 统计数据快照（JSON） */
    private String statsSnapshot;

    /** AI 生成的报告内容（JSON） */
    private String reportContent;

    /** 创建时间 */
    private LocalDateTime createdAt;
}

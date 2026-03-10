package org.ruoyi.system.crawler.domain.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 失败题目信息 VO
 *
 * @author AlpenCode
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedProblemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 失败原因
     */
    private String reason;
}

package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户刷题统计数据 VO
 * @author 32846
 */
@Data
public class AcUserStatsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 总提交次数 */
    private Integer totalSubmissions;

    /** 通过题目数（去重） */
    private Integer solvedCount;

    /** 通过率(%) */
    private Double acceptRate;

    /** 简单通过数 */
    private Integer easyCount;

    /** 中等通过数 */
    private Integer mediumCount;

    /** 困难通过数 */
    private Integer hardCount;

    /** WA 次数 */
    private Integer waCount;

    /** TLE 次数 */
    private Integer tleCount;

    /** RE 次数 */
    private Integer reCount;

    /** CE 次数 */
    private Integer ceCount;

    /** 注册天数 */
    private Integer daysSinceJoin;

    /** 各分类通过数 */
    private List<CategoryStat> categoryStats;

    /**
     * 分类维度统计
     */
    @Data
    public static class CategoryStat implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 分类名称 */
        private String name;

        /** 该分类通过题目数 */
        private Integer count;
    }
}

package org.ruoyi.system.crawler.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 爬虫请求业务对象
 *
 * @author AlpenCode
 */
@Data
public class CrawlerRequestBO {

    /**
     * 爬取题目数量，默认 20
     */
    @Min(value = 1, message = "爬取数量不能小于{value}")
    @Max(value = 100, message = "爬取数量不能大于{value}")
    private Integer limit = 20;

}

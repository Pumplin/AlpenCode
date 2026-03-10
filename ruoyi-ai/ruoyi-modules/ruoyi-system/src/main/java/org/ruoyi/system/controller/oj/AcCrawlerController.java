package org.ruoyi.system.controller.oj;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.crawler.domain.bo.CrawlerRequestBO;
import org.ruoyi.system.crawler.domain.vo.CrawlerResultVO;
import org.ruoyi.system.crawler.service.IAcCrawlerService;
import org.springframework.web.bind.annotation.*;

/**
 * LeetCode 题目爬虫控制器
 * 
 * @author AlpenCode
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/oj/crawler")
public class AcCrawlerController extends BaseController {
    
    private final IAcCrawlerService crawlerService;
    
    /**
     * 执行爬取操作
     * 
     * @param request 爬取请求参数
     * @return 爬取结果
     */
    @SaCheckPermission("oj:crawler:execute")
    @Log(title = "题目爬虫", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public R<CrawlerResultVO> execute(@Valid @RequestBody CrawlerRequestBO request) {
        // 验证 limit 参数范围
        if (request.getLimit() == null || request.getLimit() < 1 || request.getLimit() > 100) {
            return R.fail("爬取数量必须在 1-100 之间");
        }
        
        try {
            CrawlerResultVO result = crawlerService.executeCrawl(request.getLimit());
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("爬取失败: " + e.getMessage());
        }
    }
}

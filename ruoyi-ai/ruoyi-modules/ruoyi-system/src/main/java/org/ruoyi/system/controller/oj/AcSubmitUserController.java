package org.ruoyi.system.controller.oj;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.AcSubmitQueryDTO;
import org.ruoyi.system.domain.vo.AcSubmitVo;
import org.ruoyi.system.service.IAcSubmitService;
import org.springframework.web.bind.annotation.*;

/**
 * 提交记录（用户端）
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ac/submit")
public class AcSubmitUserController {

    private final IAcSubmitService submitService;

    /**
     * 轮询判题结果
     */
    @GetMapping("/info/{submitId}")
    public R<AcSubmitVo> getInfo(@PathVariable Integer submitId) {
        return R.ok(submitService.queryById(submitId));
    }

    /**
     * 当前用户的提交记录列表（分页）
     */
    @GetMapping("/page")
    public TableDataInfo<AcSubmitVo> page(AcSubmitQueryDTO queryDTO, PageQuery pageQuery) {
        Integer userId = StpUserUtil.getLoginIdAsInt();
        queryDTO.setUserId(userId);
        return submitService.queryPageList(queryDTO, pageQuery);
    }
}

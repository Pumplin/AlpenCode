package org.ruoyi.system.controller.oj;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.AcProblemCategoryQueryDTO;
import org.ruoyi.system.domain.dto.AcProblemQueryDTO;
import org.ruoyi.system.domain.vo.AcProblemCategoryVo;
import org.ruoyi.system.domain.vo.AcProblemVo;
import org.ruoyi.system.service.IAcProblemCategoryService;
import org.ruoyi.system.service.IAcProblemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 题库浏览接口（只读）
 *
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ac/problem")
public class AcProblemUserController {

    private final IAcProblemService problemService;
    private final IAcProblemCategoryService categoryService;

    /**
     * 分页查询题目列表
     */
    @GetMapping("/page")
    public TableDataInfo<AcProblemVo> page(AcProblemQueryDTO queryDTO, PageQuery pageQuery) {
        return problemService.queryPageList(queryDTO, pageQuery);
    }

    /**
     * 题目详情
     */
    @GetMapping("/info/{id}")
    public R<AcProblemVo> getInfo(@PathVariable Integer id) {
        return R.ok(problemService.queryById(id));
    }

    /**
     * 分类列表
     */
    @GetMapping("/category/list")
    public R<List<AcProblemCategoryVo>> categoryList() {
        return R.ok(categoryService.queryList(new AcProblemCategoryQueryDTO()));
    }
}

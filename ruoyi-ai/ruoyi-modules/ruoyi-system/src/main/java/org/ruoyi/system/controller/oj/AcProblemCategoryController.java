package org.ruoyi.system.controller.oj;

import cn.hutool.core.util.ObjectUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.AcProblemCategoryDTO;
import org.ruoyi.system.domain.dto.AcProblemCategoryQueryDTO;
import org.ruoyi.system.domain.vo.AcProblemCategoryVo;
import org.ruoyi.system.service.IAcProblemCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目分类管理
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/oj/category")
public class AcProblemCategoryController extends BaseController {

    private final IAcProblemCategoryService categoryService;

    /**
     * 分页查询题目分类列表
     * @param queryDTO 查询条件
     * @param pageQuery 分页参数
     * @return 分类分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<AcProblemCategoryVo> page(AcProblemCategoryQueryDTO queryDTO, PageQuery pageQuery) {
        return categoryService.queryPageList(queryDTO, pageQuery);
    }

    /**
     * 查询所有题目分类列表（不分页）
     * @param queryDTO 查询条件
     * @return 分类列表
     */
    @GetMapping("/list")
    public R<List<AcProblemCategoryVo>> list(AcProblemCategoryQueryDTO queryDTO) {
        return R.ok(categoryService.queryList(queryDTO));
    }

    /**
     * 根据ID查询题目分类详情
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    public R<AcProblemCategoryVo> getInfo(@PathVariable Integer id) {
        return R.ok(categoryService.queryById(id));
    }

    /**
     * 新增题目分类
     * @param dto 分类信息
     * @return 操作结果
     */
    @Log(title = "题目分类管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody AcProblemCategoryDTO dto) {
        return toAjax(categoryService.insertByDTO(dto));
    }

    /**
     * 修改题目分类
     * @param dto 分类信息
     * @return 操作结果
     */
    @Log(title = "题目分类管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit(@Valid @RequestBody AcProblemCategoryDTO dto) {
        if(ObjectUtil.isEmpty(dto.getId())){
            return R.fail("分类ID不能为空");
        }
        return toAjax(categoryService.updateByDTO(dto));
    }

    /**
     * 删除题目分类
     * @param ids 分类ID列表
     * @return 操作结果
     */
    @Log(title = "题目分类管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Integer> ids) {
        return toAjax(categoryService.deleteByIds(ids));
    }
}

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
import org.ruoyi.system.domain.dto.AcProblemDTO;
import org.ruoyi.system.domain.dto.AcProblemQueryDTO;
import org.ruoyi.system.domain.vo.AcProblemVo;
import org.ruoyi.system.service.IAcProblemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目管理
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/oj/problem")
public class AcProblemController extends BaseController {

    private final IAcProblemService problemService;

    /**
     * 分页查询题目列表
     * @param queryDTO 查询条件
     * @param pageQuery 分页参数
     * @return 题目分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<AcProblemVo> page(AcProblemQueryDTO queryDTO, PageQuery pageQuery) {
        return problemService.queryPageList(queryDTO, pageQuery);
    }

    /**
     * 查询所有题目列表（不分页）
     * @param queryDTO 查询条件
     * @return 题目列表
     */
    @GetMapping("/list")
    public R<List<AcProblemVo>> list(AcProblemQueryDTO queryDTO) {
        return R.ok(problemService.queryList(queryDTO));
    }

    /**
     * 根据ID查询题目详情
     * @param id 题目ID
     * @return 题目详情
     */
    @GetMapping("/{id}")
    public R<AcProblemVo> getInfo(@PathVariable Integer id) {
        return R.ok(problemService.queryById(id));
    }

    /**
     * 新增题目
     * @param dto 题目信息
     * @return 操作结果
     */
    @Log(title = "题目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody AcProblemDTO dto) {
        if(ObjectUtil.isEmpty(dto.getTitle())){
            return R.fail("题目标题不能为空");
        }
        if(ObjectUtil.isEmpty(dto.getDescription())){
            return R.fail("题目描述不能为空");
        }
        if(ObjectUtil.isEmpty(dto.getDifficulty())){
            return R.fail("题目难度不能为空");
        }
        return toAjax(problemService.insertByDTO(dto));
    }

    /**
     * 修改题目
     * @param dto 题目信息
     * @return 操作结果
     */
    @Log(title = "题目管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit(@Valid @RequestBody AcProblemDTO dto) {
        if(ObjectUtil.isEmpty(dto.getId())){
            return R.fail("题目ID不能为空");
        }
        return toAjax(problemService.updateByDTO(dto));
    }

    /**
     * 删除题目
     * @param ids 题目ID列表
     * @return 操作结果
     */
    @Log(title = "题目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Integer> ids) {
        return toAjax(problemService.deleteByIds(ids));
    }
}

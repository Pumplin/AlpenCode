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
import org.ruoyi.system.domain.dto.AcSubmitDTO;
import org.ruoyi.system.domain.dto.AcSubmitQueryDTO;
import org.ruoyi.system.domain.vo.AcSubmitVo;
import org.ruoyi.system.service.IAcSubmitService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提交记录管理
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/oj/submit")
public class AcSubmitController extends BaseController {

    private final IAcSubmitService submitService;

    /**
     * 分页查询提交记录列表
     * @param queryDTO 查询条件
     * @param pageQuery 分页参数
     * @return 提交记录分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<AcSubmitVo> page(AcSubmitQueryDTO queryDTO, PageQuery pageQuery) {
        return submitService.queryPageList(queryDTO, pageQuery);
    }

    /**
     * 查询所有提交记录列表（不分页）
     * @param queryDTO 查询条件
     * @return 提交记录列表
     */
    @GetMapping("/list")
    public R<List<AcSubmitVo>> list(AcSubmitQueryDTO queryDTO) {
        return R.ok(submitService.queryList(queryDTO));
    }

    /**
     * 根据ID查询提交记录详情
     * @param id 提交记录ID
     * @return 提交记录详情
     */
    @GetMapping("/{id}")
    public R<AcSubmitVo> getInfo(@PathVariable Integer id) {
        return R.ok(submitService.queryById(id));
    }

    /**
     * 新增提交记录
     * @param dto 提交记录信息
     * @return 操作结果
     */
    @Log(title = "提交记录管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody AcSubmitDTO dto) {
        return toAjax(submitService.insertByDTO(dto));
    }

    /**
     * 修改提交记录
     * @param dto 提交记录信息
     * @return 操作结果
     */
    @Log(title = "提交记录管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit(@Valid @RequestBody AcSubmitDTO dto) {
        if(ObjectUtil.isEmpty(dto.getId())){
            return R.fail("提交记录ID不能为空");
        }
        return toAjax(submitService.updateByDTO(dto));
    }

    /**
     * 删除提交记录
     * @param ids 提交记录ID列表
     * @return 操作结果
     */
    @Log(title = "提交记录管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Integer> ids) {
        return toAjax(submitService.deleteByIds(ids));
    }
}

package org.ruoyi.system.controller.oj;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.dto.AcTestCaseDTO;
import org.ruoyi.system.domain.dto.AcTestCaseQueryDTO;
import org.ruoyi.system.domain.vo.AcTestCaseVo;
import org.ruoyi.system.service.IAcTestCaseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试用例管理
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/oj/testCase")
public class AcTestCaseController extends BaseController {

    private final IAcTestCaseService testCaseService;

    /**
     * 分页查询测试用例列表
     * @param queryDTO 查询条件
     * @param pageQuery 分页参数
     * @return 测试用例分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<AcTestCaseVo> page(AcTestCaseQueryDTO queryDTO, PageQuery pageQuery) {
        return testCaseService.queryPageList(queryDTO, pageQuery);
    }

    /**
     * 查询所有测试用例列表（不分页）
     * @param queryDTO 查询条件
     * @return 测试用例列表
     */
    @GetMapping("/list")
    public R<List<AcTestCaseVo>> list(AcTestCaseQueryDTO queryDTO) {
        return R.ok(testCaseService.queryList(queryDTO));
    }

    /**
     * 根据ID查询测试用例详情
     * @param id 测试用例ID
     * @return 测试用例详情
     */
    @GetMapping("/{id}")
    public R<AcTestCaseVo> getInfo(@PathVariable Integer id) {
        return R.ok(testCaseService.queryById(id));
    }

    /**
     * 新增测试用例
     * @param dto 测试用例信息
     * @return 操作结果
     */
    @Log(title = "测试用例管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody AcTestCaseDTO dto) {
        return toAjax(testCaseService.insertByDTO(dto));
    }

    /**
     * 修改测试用例
     * @param dto 测试用例信息
     * @return 操作结果
     */
    @Log(title = "测试用例管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit(@Valid @RequestBody AcTestCaseDTO dto) {
        if(dto.getId() == null){
            return R.fail("测试用例ID不能为空");
        }
        return toAjax(testCaseService.updateByDTO(dto));
    }

    /**
     * 删除测试用例
     * @param ids 测试用例ID列表
     * @return 操作结果
     */
    @Log(title = "测试用例管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Integer> ids) {
        return toAjax(testCaseService.deleteByIds(ids));
    }
}

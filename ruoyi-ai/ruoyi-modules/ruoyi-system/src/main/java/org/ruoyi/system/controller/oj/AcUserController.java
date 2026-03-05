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
import org.ruoyi.system.domain.dto.AcUserDTO;
import org.ruoyi.system.domain.dto.AcUserQueryDTO;
import org.ruoyi.system.domain.vo.AcUserVo;
import org.ruoyi.system.service.IAcUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OJ用户管理
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/oj/user")
public class AcUserController extends BaseController {

    private final IAcUserService userService;

    /**
     * 分页查询OJ用户列表
     * @param queryDTO 查询条件
     * @param pageQuery 分页参数
     * @return 用户分页列表
     */
    @GetMapping("/page")
    public TableDataInfo<AcUserVo> page(AcUserQueryDTO queryDTO, PageQuery pageQuery) {
        return userService.queryPageList(queryDTO, pageQuery);
    }

    /**
     * 查询所有OJ用户列表（不分页）
     * @param queryDTO 查询条件
     * @return 用户列表
     */
    @GetMapping("/list")
    public R<List<AcUserVo>> list(AcUserQueryDTO queryDTO) {
        return R.ok(userService.queryList(queryDTO));
    }

    /**
     * 根据ID查询OJ用户详情
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public R<AcUserVo> getInfo(@PathVariable Integer id) {
        return R.ok(userService.queryById(id));
    }

    /**
     * 新增OJ用户
     * @param dto 用户信息
     * @return 操作结果
     */
    @Log(title = "OJ用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody AcUserDTO dto) {
        return toAjax(userService.insertByDTO(dto));
    }

    /**
     * 修改OJ用户
     * @param dto 用户信息
     * @return 操作结果
     */
    @Log(title = "OJ用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit(@Valid @RequestBody AcUserDTO dto) {
        if(ObjectUtil.isEmpty(dto.getId())){
            return R.fail("用户ID不能为空");
        }
        return toAjax(userService.updateByDTO(dto));
    }

    /**
     * 删除OJ用户
     * @param ids 用户ID列表
     * @return 操作结果
     */
    @Log(title = "OJ用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Integer> ids) {
        return toAjax(userService.deleteByIds(ids));
    }
}

package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AcTestCase;
import org.ruoyi.system.domain.dto.AcTestCaseDTO;
import org.ruoyi.system.domain.dto.AcTestCaseQueryDTO;
import org.ruoyi.system.domain.vo.AcTestCaseVo;
import org.ruoyi.system.mapper.AcTestCaseMapper;
import org.ruoyi.system.service.IAcProblemService;
import org.ruoyi.system.service.IAcTestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author 32846
 */
@RequiredArgsConstructor
@Service
public class AcTestCaseServiceImpl extends ServiceImpl<AcTestCaseMapper, AcTestCase>
        implements IAcTestCaseService {

    @Autowired
    private IAcProblemService acProblemService;

    @Override
    public AcTestCaseVo queryById(Integer id) {
        AcTestCase entity = this.getById(id);
        return BeanUtil.toBean(entity, AcTestCaseVo.class);
    }

    @Override
    public TableDataInfo<AcTestCaseVo> queryPageList(AcTestCaseQueryDTO queryDTO, PageQuery pageQuery) {
        LambdaQueryWrapper<AcTestCase> lqw = buildQueryWrapper(queryDTO);
        Page<AcTestCase> page = this.page(pageQuery.build(), lqw);
        Page<AcTestCaseVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(BeanUtil.copyToList(page.getRecords(), AcTestCaseVo.class));
        return TableDataInfo.build(voPage);
    }

    @Override
    public List<AcTestCaseVo> queryList(AcTestCaseQueryDTO queryDTO) {
        LambdaQueryWrapper<AcTestCase> lqw = buildQueryWrapper(queryDTO);
        List<AcTestCase> list = this.list(lqw);
        return BeanUtil.copyToList(list, AcTestCaseVo.class);
    }

    private LambdaQueryWrapper<AcTestCase> buildQueryWrapper(AcTestCaseQueryDTO queryDTO) {
        LambdaQueryWrapper<AcTestCase> lqw = Wrappers.lambdaQuery();
        lqw.eq(queryDTO.getProblemId() != null, AcTestCase::getProblemId, queryDTO.getProblemId());
        lqw.eq(queryDTO.getIsSample() != null, AcTestCase::getIsSample, queryDTO.getIsSample());
        lqw.eq(queryDTO.getStatus() != null, AcTestCase::getStatus, queryDTO.getStatus());
        lqw.orderByAsc(AcTestCase::getSort);
        return lqw;
    }

    @Override
    public Boolean insertByDTO(AcTestCaseDTO dto) {
        if(ObjectUtil.isEmpty(acProblemService.getById(dto.getProblemId()))){
            throw new ServiceException("该题目不存在");
        }
        AcTestCase add = BeanUtil.toBean(dto, AcTestCase.class);
        add.setIsDelete(CommonConstants.NOT_DELETE);
        add.setStatus(dto.getStatus()!= null?dto.getStatus(): CommonConstants.IS_AVAILABLE);
        return this.save(add);
    }

    @Override
    public Boolean updateByDTO(AcTestCaseDTO dto) {
        AcTestCase update = BeanUtil.toBean(dto, AcTestCase.class);
        return this.updateById(update);
    }

    @Override
    public Boolean deleteByIds(Collection<Integer> ids) {
        ids.forEach(id -> {
            if (this.getById(id) == null) {
                throw new ServiceException("测试用例 id=" + id + " 不存在");
            }
        });
        return this.removeByIds(ids);
    }
}

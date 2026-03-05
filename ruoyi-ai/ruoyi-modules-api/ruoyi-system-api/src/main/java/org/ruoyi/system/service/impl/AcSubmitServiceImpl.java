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
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AcSubmit;
import org.ruoyi.system.domain.dto.AcSubmitDTO;
import org.ruoyi.system.domain.dto.AcSubmitQueryDTO;
import org.ruoyi.system.domain.vo.AcSubmitVo;
import org.ruoyi.system.mapper.AcSubmitMapper;
import org.ruoyi.system.service.IAcProblemService;
import org.ruoyi.system.service.IAcSubmitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author 32846
 */
@RequiredArgsConstructor
@Service
public class AcSubmitServiceImpl extends ServiceImpl<AcSubmitMapper, AcSubmit>
        implements IAcSubmitService {

    @Autowired
    private IAcProblemService acProblemService;

    @Override
    public AcSubmitVo queryById(Integer id) {
        AcSubmit entity = this.getById(id);
        return BeanUtil.toBean(entity, AcSubmitVo.class);
    }

    @Override
    public TableDataInfo<AcSubmitVo> queryPageList(AcSubmitQueryDTO queryDTO, PageQuery pageQuery) {
        LambdaQueryWrapper<AcSubmit> lqw = buildQueryWrapper(queryDTO);
        Page<AcSubmit> page = this.page(pageQuery.build(), lqw);
        Page<AcSubmitVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(BeanUtil.copyToList(page.getRecords(), AcSubmitVo.class));
        return TableDataInfo.build(voPage);
    }

    @Override
    public List<AcSubmitVo> queryList(AcSubmitQueryDTO queryDTO) {
        LambdaQueryWrapper<AcSubmit> lqw = buildQueryWrapper(queryDTO);
        List<AcSubmit> list = this.list(lqw);
        return BeanUtil.copyToList(list, AcSubmitVo.class);
    }

    private LambdaQueryWrapper<AcSubmit> buildQueryWrapper(AcSubmitQueryDTO queryDTO) {
        LambdaQueryWrapper<AcSubmit> lqw = Wrappers.lambdaQuery();
        lqw.eq(queryDTO.getUserId() != null, AcSubmit::getUserId, queryDTO.getUserId());
        lqw.eq(queryDTO.getProblemId() != null, AcSubmit::getProblemId, queryDTO.getProblemId());
        lqw.eq(StringUtils.isNotBlank(queryDTO.getLanguage()), AcSubmit::getLanguage, queryDTO.getLanguage());
        lqw.eq(queryDTO.getResult() != null, AcSubmit::getResult, queryDTO.getResult());
        lqw.orderByDesc(AcSubmit::getCreatedAt);
        return lqw;
    }

    @Override
    public Boolean insertByDTO(AcSubmitDTO dto) {
        if(ObjectUtil.isEmpty(acProblemService.getById(dto.getProblemId()))){
            throw new ServiceException("该题目不存在");
        }
        AcSubmit add = BeanUtil.toBean(dto, AcSubmit.class);
        add.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(add);
    }

    @Override
    public Boolean updateByDTO(AcSubmitDTO dto) {
        AcSubmit update = BeanUtil.toBean(dto, AcSubmit.class);
        return this.updateById(update);
    }

    @Override
    public Boolean deleteByIds(Collection<Integer> ids) {
        ids.forEach(id -> {
            if (this.getById(id) == null) {
                throw new ServiceException("提交记录 id=" + id + " 不存在");
            }
        });
        return this.removeByIds(ids);
    }
}

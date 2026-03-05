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
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.AcProblemCategory;
import org.ruoyi.system.domain.AcProblemCategoryMap;
import org.ruoyi.system.domain.dto.AcProblemCategoryDTO;
import org.ruoyi.system.domain.dto.AcProblemCategoryQueryDTO;
import org.ruoyi.system.domain.vo.AcProblemCategoryVo;
import org.ruoyi.system.mapper.AcProblemCategoryMapper;
import org.ruoyi.system.service.IAcProblemCategoryMapService;
import org.ruoyi.system.service.IAcProblemCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author 32846
 */
@RequiredArgsConstructor
@Service
public class AcProblemCategoryServiceImpl extends ServiceImpl<AcProblemCategoryMapper, AcProblemCategory>
        implements IAcProblemCategoryService {

    @Autowired
    private IAcProblemCategoryMapService problemCategoryMapService;

    @Override
    public AcProblemCategoryVo queryById(Integer id) {
        AcProblemCategory entity = this.getById(id);
        return BeanUtil.toBean(entity, AcProblemCategoryVo.class);
    }

    @Override
    public TableDataInfo<AcProblemCategoryVo> queryPageList(AcProblemCategoryQueryDTO queryDTO, PageQuery pageQuery) {
        LambdaQueryWrapper<AcProblemCategory> lqw = buildQueryWrapper(queryDTO);
        Page<AcProblemCategory> page = this.page(pageQuery.build(), lqw);
        Page<AcProblemCategoryVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(BeanUtil.copyToList(page.getRecords(), AcProblemCategoryVo.class));
        return TableDataInfo.build(voPage);
    }

    @Override
    public List<AcProblemCategoryVo> queryList(AcProblemCategoryQueryDTO queryDTO) {
        LambdaQueryWrapper<AcProblemCategory> lqw = buildQueryWrapper(queryDTO);
        List<AcProblemCategory> list = this.list(lqw);
        return BeanUtil.copyToList(list, AcProblemCategoryVo.class);
    }

    private LambdaQueryWrapper<AcProblemCategory> buildQueryWrapper(AcProblemCategoryQueryDTO queryDTO) {
        LambdaQueryWrapper<AcProblemCategory> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(queryDTO.getName()), AcProblemCategory::getName, queryDTO.getName());
        lqw.orderByDesc(AcProblemCategory::getCreatedAt);
        return lqw;
    }

    @Override
    public Boolean insertByDTO(AcProblemCategoryDTO dto) {
        AcProblemCategory add = BeanUtil.toBean(dto, AcProblemCategory.class);
        add.setIsDelete(CommonConstants.NOT_DELETE);
        return this.save(add);
    }

    @Override
    public Boolean updateByDTO(AcProblemCategoryDTO dto) {
        if(ObjectUtil.isEmpty(this.getById(dto.getId()))){
            throw new ServiceException("该分类不存在");
        }
        AcProblemCategory update = BeanUtil.toBean(dto, AcProblemCategory.class);
        return this.updateById(update);
    }

    @Override
    public Boolean deleteByIds(Collection<Integer> ids) {
        ids.forEach(id -> {
            if (this.getById(id) == null) {
                throw new ServiceException("分类 id=" + id + " 不存在");
            }
            long count = problemCategoryMapService.count(
                Wrappers.lambdaQuery(AcProblemCategoryMap.class)
                    .eq(AcProblemCategoryMap::getCategoryId, id));
            if (count > 0) {
                throw new ServiceException("分类 id=" + id + " 下存在关联题目，请先解除关联");
            }
        });
        return this.removeByIds(ids);
    }
}

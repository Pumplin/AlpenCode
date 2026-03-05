package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import org.ruoyi.system.domain.dto.AcProblemDTO;
import org.ruoyi.system.domain.dto.AcProblemQueryDTO;
import org.ruoyi.system.domain.vo.AcProblemCategoryVo;
import org.ruoyi.system.domain.vo.AcProblemVo;
import org.ruoyi.system.mapper.AcProblemMapper;
import org.ruoyi.system.service.IAcProblemCategoryMapService;
import org.ruoyi.system.service.IAcProblemCategoryService;
import org.ruoyi.system.service.IAcProblemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 32846
 */
@RequiredArgsConstructor
@Service
public class AcProblemServiceImpl extends ServiceImpl<AcProblemMapper, AcProblem>
        implements IAcProblemService {

    private final IAcProblemCategoryMapService categoryMapService;
    private final IAcProblemCategoryService categoryService;

    @Override
    public AcProblemVo queryById(Integer id) {
        AcProblem entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        AcProblemVo vo = BeanUtil.toBean(entity, AcProblemVo.class);
        vo.setCategories(getCategoriesByProblemId(id));
        return vo;
    }

    @Override
    public TableDataInfo<AcProblemVo> queryPageList(AcProblemQueryDTO queryDTO, PageQuery pageQuery) {
        // 如果按分类筛选，先查出该分类下的题目ID
        List<Integer> problemIds = null;
        if (queryDTO.getCategoryId() != null) {
            List<AcProblemCategoryMap> maps = categoryMapService.list(
                Wrappers.lambdaQuery(AcProblemCategoryMap.class)
                    .eq(AcProblemCategoryMap::getCategoryId, queryDTO.getCategoryId()));
            problemIds = maps.stream().map(AcProblemCategoryMap::getProblemId).collect(Collectors.toList());
            if (problemIds.isEmpty()) {
                return TableDataInfo.build(new Page<>());
            }
        }

        LambdaQueryWrapper<AcProblem> lqw = buildQueryWrapper(queryDTO);
        if (problemIds != null) {
            lqw.in(AcProblem::getId, problemIds);
        }
        Page<AcProblem> page = this.page(pageQuery.build(), lqw);
        Page<AcProblemVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AcProblemVo> voList = BeanUtil.copyToList(page.getRecords(), AcProblemVo.class);
        voList.forEach(vo -> vo.setCategories(getCategoriesByProblemId(vo.getId())));
        voPage.setRecords(voList);
        return TableDataInfo.build(voPage);
    }

    @Override
    public List<AcProblemVo> queryList(AcProblemQueryDTO queryDTO) {
        LambdaQueryWrapper<AcProblem> lqw = buildQueryWrapper(queryDTO);
        List<AcProblem> list = this.list(lqw);
        return BeanUtil.copyToList(list, AcProblemVo.class);
    }

    private LambdaQueryWrapper<AcProblem> buildQueryWrapper(AcProblemQueryDTO queryDTO) {
        LambdaQueryWrapper<AcProblem> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(queryDTO.getTitle()), AcProblem::getTitle, queryDTO.getTitle());
        lqw.eq(queryDTO.getDifficulty() != null, AcProblem::getDifficulty, queryDTO.getDifficulty());
        lqw.eq(queryDTO.getStatus() != null, AcProblem::getStatus, queryDTO.getStatus());
        lqw.orderByDesc(AcProblem::getCreatedAt);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByDTO(AcProblemDTO dto) {
        if (CollUtil.isNotEmpty(dto.getCategoryIds())) {
            validateCategoryIds(dto.getCategoryIds());
        }
        AcProblem add = BeanUtil.toBean(dto, AcProblem.class);
        add.setStatus(dto.getStatus()!= null?dto.getStatus(): CommonConstants.IS_AVAILABLE);
        add.setIsDelete(CommonConstants.NOT_DELETE);
        boolean flag = this.save(add);
        if (flag && CollUtil.isNotEmpty(dto.getCategoryIds())) {
            insertCategoryMap(add.getId(), dto.getCategoryIds());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByDTO(AcProblemDTO dto) {
        if (CollUtil.isNotEmpty(dto.getCategoryIds())) {
            validateCategoryIds(dto.getCategoryIds());
        }
        AcProblem update = BeanUtil.toBean(dto, AcProblem.class);
        boolean flag = this.updateById(update);
        if (flag && dto.getCategoryIds() != null) {
            categoryMapService.remove(Wrappers.lambdaQuery(AcProblemCategoryMap.class)
                .eq(AcProblemCategoryMap::getProblemId, dto.getId()));
            if (CollUtil.isNotEmpty(dto.getCategoryIds())) {
                insertCategoryMap(dto.getId(), dto.getCategoryIds());
            }
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(Collection<Integer> ids) {
        ids.forEach(id -> {
            if (this.getById(id) == null) {
                throw new ServiceException("题目 id=" + id + " 不存在");
            }
        });
        categoryMapService.remove(Wrappers.lambdaQuery(AcProblemCategoryMap.class)
            .in(AcProblemCategoryMap::getProblemId, ids));
        return this.removeByIds(ids);
    }

    private void validateCategoryIds(List<Integer> categoryIds) {
        categoryIds.forEach(catId -> {
            if (categoryService.getById(catId) == null) {
                throw new ServiceException("分类 id=" + catId + " 不存在");
            }
        });
    }

    private void insertCategoryMap(Integer problemId, List<Integer> categoryIds) {
        List<AcProblemCategoryMap> maps = categoryIds.stream().map(catId -> {
            AcProblemCategoryMap map = new AcProblemCategoryMap();
            map.setProblemId(problemId);
            map.setCategoryId(catId);
            return map;
        }).collect(Collectors.toList());
        categoryMapService.saveBatch(maps);
    }

    private List<AcProblemCategoryVo> getCategoriesByProblemId(Integer problemId) {
        List<AcProblemCategoryMap> maps = categoryMapService.list(
            Wrappers.lambdaQuery(AcProblemCategoryMap.class)
                .eq(AcProblemCategoryMap::getProblemId, problemId));
        if (CollUtil.isEmpty(maps)) {
            return Collections.emptyList();
        }
        List<Integer> categoryIds = maps.stream()
            .map(AcProblemCategoryMap::getCategoryId)
            .collect(Collectors.toList());
        List<AcProblemCategory> categories = categoryService.list(
            Wrappers.lambdaQuery(AcProblemCategory.class)
                .in(AcProblemCategory::getId, categoryIds));
        return BeanUtil.copyToList(categories, AcProblemCategoryVo.class);
    }
}

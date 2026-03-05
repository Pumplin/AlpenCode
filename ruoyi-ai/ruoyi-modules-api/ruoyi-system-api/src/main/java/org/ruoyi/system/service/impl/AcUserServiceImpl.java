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
import org.ruoyi.system.domain.AcUser;
import org.ruoyi.system.domain.dto.AcUserDTO;
import org.ruoyi.system.domain.dto.AcUserQueryDTO;
import org.ruoyi.system.domain.vo.AcUserVo;
import org.ruoyi.system.mapper.AcUserMapper;
import org.ruoyi.system.service.IAcUserService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author 32846
 */
@RequiredArgsConstructor
@Service
public class AcUserServiceImpl extends ServiceImpl<AcUserMapper, AcUser> implements IAcUserService {

    @Override
    public AcUserVo queryById(Integer id) {
        AcUser entity = this.getById(id);
        return BeanUtil.toBean(entity, AcUserVo.class);
    }

    @Override
    public TableDataInfo<AcUserVo> queryPageList(AcUserQueryDTO queryDTO, PageQuery pageQuery) {
        LambdaQueryWrapper<AcUser> lqw = buildQueryWrapper(queryDTO);
        Page<AcUser> page = this.page(pageQuery.build(), lqw);
        Page<AcUserVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(BeanUtil.copyToList(page.getRecords(), AcUserVo.class));
        return TableDataInfo.build(voPage);
    }

    @Override
    public List<AcUserVo> queryList(AcUserQueryDTO queryDTO) {
        LambdaQueryWrapper<AcUser> lqw = buildQueryWrapper(queryDTO);
        List<AcUser> list = this.list(lqw);
        return BeanUtil.copyToList(list, AcUserVo.class);
    }

    @Override
    public Boolean insertByDTO(AcUserDTO dto) {
        AcUser add = BeanUtil.toBean(dto, AcUser.class);
        add.setIsDelete(CommonConstants.NOT_DELETE);
        add.setStatus(dto.getStatus()!= null?dto.getStatus():CommonConstants.IS_AVAILABLE);
        return this.save(add);
    }

    @Override
    public Boolean updateByDTO(AcUserDTO dto) {
        if(ObjectUtil.isEmpty(this.getById(dto.getId()))){
            throw new ServiceException("用户不存在");
        }
        AcUser update = BeanUtil.toBean(dto, AcUser.class);
        return this.updateById(update);
    }

    @Override
    public Boolean deleteByIds(Collection<Integer> ids) {
        ids.forEach(id -> {
            if (this.getById(id) == null) {
                throw new ServiceException("用户 id=" + id + " 不存在");
            }
        });
        return this.removeByIds(ids);
    }

    private LambdaQueryWrapper<AcUser> buildQueryWrapper(AcUserQueryDTO queryDTO) {
        LambdaQueryWrapper<AcUser> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(queryDTO.getUsername()), AcUser::getUsername, queryDTO.getUsername());
        lqw.like(StringUtils.isNotBlank(queryDTO.getEmail()), AcUser::getEmail, queryDTO.getEmail());
        lqw.eq(ObjectUtil.isNotEmpty(queryDTO.getStatus()), AcUser::getStatus, queryDTO.getStatus());
        lqw.orderByDesc(AcUser::getCreatedAt);
        return lqw;
    }
}

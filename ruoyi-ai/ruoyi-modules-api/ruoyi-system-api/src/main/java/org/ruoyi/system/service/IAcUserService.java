package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AcUser;
import org.ruoyi.system.domain.dto.AcUserDTO;
import org.ruoyi.system.domain.dto.AcUserQueryDTO;
import org.ruoyi.system.domain.vo.AcUserVo;

import java.util.Collection;
import java.util.List;

/**
 * OJ用户Service接口
 * @author 32846
 */
public interface IAcUserService extends IService<AcUser> {

    AcUserVo queryById(Integer id);

    TableDataInfo<AcUserVo> queryPageList(AcUserQueryDTO queryDTO, PageQuery pageQuery);

    List<AcUserVo> queryList(AcUserQueryDTO queryDTO);

    Boolean insertByDTO(AcUserDTO dto);

    Boolean updateByDTO(AcUserDTO dto);

    Boolean deleteByIds(Collection<Integer> ids);
}

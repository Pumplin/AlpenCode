package org.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.AcUser;
import org.ruoyi.system.domain.vo.AcUserVo;

/**
 * OJ用户Mapper接口
 * @author 32846
 */
@Mapper
public interface AcUserMapper extends BaseMapperPlus<AcUser, AcUserVo> {
}

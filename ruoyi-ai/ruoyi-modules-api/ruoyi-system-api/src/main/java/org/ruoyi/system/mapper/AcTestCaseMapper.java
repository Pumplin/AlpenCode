package org.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.AcTestCase;
import org.ruoyi.system.domain.vo.AcTestCaseVo;

/**
 * 测试用例Mapper接口
 * @author 32846
 */
@Mapper
public interface AcTestCaseMapper extends BaseMapperPlus<AcTestCase, AcTestCaseVo> {
}

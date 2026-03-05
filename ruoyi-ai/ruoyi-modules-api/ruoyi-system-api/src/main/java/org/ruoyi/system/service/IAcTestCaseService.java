package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AcTestCase;
import org.ruoyi.system.domain.dto.AcTestCaseDTO;
import org.ruoyi.system.domain.dto.AcTestCaseQueryDTO;
import org.ruoyi.system.domain.vo.AcTestCaseVo;

import java.util.Collection;
import java.util.List;

/**
 * 测试用例Service接口
 * @author 32846
 */
public interface IAcTestCaseService extends IService<AcTestCase> {

    AcTestCaseVo queryById(Integer id);

    TableDataInfo<AcTestCaseVo> queryPageList(AcTestCaseQueryDTO queryDTO, PageQuery pageQuery);

    List<AcTestCaseVo> queryList(AcTestCaseQueryDTO queryDTO);

    Boolean insertByDTO(AcTestCaseDTO dto);

    Boolean updateByDTO(AcTestCaseDTO dto);

    Boolean deleteByIds(Collection<Integer> ids);
}

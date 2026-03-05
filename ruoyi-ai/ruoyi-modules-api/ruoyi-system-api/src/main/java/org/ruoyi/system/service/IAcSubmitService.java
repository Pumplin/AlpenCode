package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AcSubmit;
import org.ruoyi.system.domain.dto.AcSubmitDTO;
import org.ruoyi.system.domain.dto.AcSubmitQueryDTO;
import org.ruoyi.system.domain.vo.AcSubmitVo;

import java.util.Collection;
import java.util.List;

/**
 * 提交记录Service接口
 * @author 32846
 */
public interface IAcSubmitService extends IService<AcSubmit> {

    AcSubmitVo queryById(Integer id);

    TableDataInfo<AcSubmitVo> queryPageList(AcSubmitQueryDTO queryDTO, PageQuery pageQuery);

    List<AcSubmitVo> queryList(AcSubmitQueryDTO queryDTO);

    Boolean insertByDTO(AcSubmitDTO dto);

    Boolean updateByDTO(AcSubmitDTO dto);

    Boolean deleteByIds(Collection<Integer> ids);
}

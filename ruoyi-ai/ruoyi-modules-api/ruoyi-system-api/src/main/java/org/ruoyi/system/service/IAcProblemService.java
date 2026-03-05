package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AcProblem;
import org.ruoyi.system.domain.dto.AcProblemDTO;
import org.ruoyi.system.domain.dto.AcProblemQueryDTO;
import org.ruoyi.system.domain.vo.AcProblemVo;

import java.util.Collection;
import java.util.List;

/**
 * 题目Service接口
 * @author 32846
 */
public interface IAcProblemService extends IService<AcProblem> {

    AcProblemVo queryById(Integer id);

    TableDataInfo<AcProblemVo> queryPageList(AcProblemQueryDTO queryDTO, PageQuery pageQuery);

    List<AcProblemVo> queryList(AcProblemQueryDTO queryDTO);

    Boolean insertByDTO(AcProblemDTO dto);

    Boolean updateByDTO(AcProblemDTO dto);

    Boolean deleteByIds(Collection<Integer> ids);
}

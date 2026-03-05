package org.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AcProblemCategory;
import org.ruoyi.system.domain.dto.AcProblemCategoryDTO;
import org.ruoyi.system.domain.dto.AcProblemCategoryQueryDTO;
import org.ruoyi.system.domain.vo.AcProblemCategoryVo;

import java.util.Collection;
import java.util.List;

/**
 * 题目分类Service接口
 * @author 32846
 */
public interface IAcProblemCategoryService extends IService<AcProblemCategory> {

    AcProblemCategoryVo queryById(Integer id);

    TableDataInfo<AcProblemCategoryVo> queryPageList(AcProblemCategoryQueryDTO queryDTO, PageQuery pageQuery);

    List<AcProblemCategoryVo> queryList(AcProblemCategoryQueryDTO queryDTO);

    Boolean insertByDTO(AcProblemCategoryDTO dto);

    Boolean updateByDTO(AcProblemCategoryDTO dto);

    Boolean deleteByIds(Collection<Integer> ids);
}

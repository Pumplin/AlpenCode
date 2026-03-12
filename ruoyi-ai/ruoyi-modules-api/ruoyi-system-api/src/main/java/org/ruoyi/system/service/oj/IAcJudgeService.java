package org.ruoyi.system.service.oj;

import org.ruoyi.system.domain.dto.JudgeRequestDTO;
import org.ruoyi.system.domain.vo.RunCodeResultVO;

import java.util.List;

/**
 * 判题 Service
 * @author 32846
 */
public interface IAcJudgeService {

    /**
     * Run Code：只跑公开样例（is_sample=1），同步返回结果，不记录提交
     */
    List<RunCodeResultVO> runCode(JudgeRequestDTO dto);

    /**
     * Submit：创建提交记录，发 MQ 消息，返回 submitId
     */
    Integer submit(JudgeRequestDTO dto, Integer userId);
}

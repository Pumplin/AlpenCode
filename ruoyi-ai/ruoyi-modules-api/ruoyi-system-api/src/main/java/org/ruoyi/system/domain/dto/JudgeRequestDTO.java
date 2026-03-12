package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 判题请求 DTO（Run Code 和 Submit 共用）
 * @author 32846
 */
@Data
public class JudgeRequestDTO {

    @NotNull(message = "题目ID不能为空")
    private Integer problemId;

    @NotBlank(message = "语言不能为空")
    private String language;

    @NotBlank(message = "代码不能为空")
    private String code;
}

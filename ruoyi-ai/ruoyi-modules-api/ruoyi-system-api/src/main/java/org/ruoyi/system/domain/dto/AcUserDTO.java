package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * OJ用户新增/修改DTO
 * @author 32846
 */
@Data
public class AcUserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID（修改时必传） */
    private Integer id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String passwordHash;

    private String email;

    private Integer status;
}

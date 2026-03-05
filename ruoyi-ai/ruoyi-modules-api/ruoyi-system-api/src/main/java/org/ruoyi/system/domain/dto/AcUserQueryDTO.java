package org.ruoyi.system.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * OJ用户查询条件DTO
 * @author 32846
 */
@Data
public class AcUserQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户名（模糊查询） */
    private String username;

    /** 邮箱（模糊查询） */
    private String email;

    /** 状态 */
    private Integer status;
}

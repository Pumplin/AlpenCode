package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户端登录响应对象
 *
 * @author 32846
 */
@Data
public class AcLoginVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户端 Token */
    private String token;

    /** 用户基本信息 */
    private AcUserVo user;
}

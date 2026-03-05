package org.ruoyi.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.AcUser;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OJ用户视图对象 ac_user
 * @author 32846
 */
@Data
public class AcUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String username;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

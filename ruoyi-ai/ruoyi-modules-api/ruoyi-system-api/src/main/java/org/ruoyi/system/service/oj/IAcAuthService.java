package org.ruoyi.system.service.oj;

import org.ruoyi.system.domain.dto.AcUserLoginDTO;
import org.ruoyi.system.domain.dto.AcUserRegisterDTO;
import org.ruoyi.system.domain.vo.AcLoginVo;
import org.ruoyi.system.domain.vo.AcUserVo;

/**
 * 用户端认证Service接口
 *
 * @author 32846
 */
public interface IAcAuthService {

    /**
     * 用户注册
     *
     * @param dto 注册信息
     */
    void register(AcUserRegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return Token + 用户信息
     */
    AcLoginVo login(AcUserLoginDTO dto);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    AcUserVo getLoginUserInfo();
}

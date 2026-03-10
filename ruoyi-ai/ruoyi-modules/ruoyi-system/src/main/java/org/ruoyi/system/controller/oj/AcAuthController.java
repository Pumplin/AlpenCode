package org.ruoyi.system.controller.oj;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.system.domain.dto.AcUserLoginDTO;
import org.ruoyi.system.domain.dto.AcUserRegisterDTO;
import org.ruoyi.system.domain.vo.AcLoginVo;
import org.ruoyi.system.domain.vo.AcUserVo;
import org.ruoyi.system.service.oj.IAcAuthService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端认证控制器
 *
 * @author 32846
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ac/auth")
public class AcAuthController {

    private final IAcAuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<String> register(@Valid @RequestBody AcUserRegisterDTO dto) {
        authService.register(dto);
        return R.ok("注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<AcLoginVo> login(@Valid @RequestBody AcUserLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public R<AcUserVo> info() {
        return R.ok(authService.getLoginUserInfo());
    }
}

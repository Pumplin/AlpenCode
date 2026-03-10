package org.ruoyi.system.service.oj.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.CommonConstants;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.system.domain.AcUser;
import org.ruoyi.system.domain.dto.AcUserLoginDTO;
import org.ruoyi.system.domain.dto.AcUserRegisterDTO;
import org.ruoyi.system.domain.vo.AcLoginVo;
import org.ruoyi.system.domain.vo.AcUserVo;
import org.ruoyi.system.mapper.AcUserMapper;
import org.ruoyi.system.service.oj.IAcAuthService;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * 用户端认证Service实现
 *
 * @author 32846
 */
@RequiredArgsConstructor
@Service
public class AcAuthServiceImpl implements IAcAuthService {

    private final AcUserMapper acUserMapper;

    @Override
    public void register(AcUserRegisterDTO dto) {
        // 校验用户名是否已存在
        Long count = acUserMapper.selectCount(
            Wrappers.<AcUser>lambdaQuery().eq(AcUser::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new ServiceException("用户名已存在");
        }

        // 对前端 Base64 编码的密码进行解码
        String rawPassword = new String(Base64.getDecoder().decode(dto.getPassword()));

        // 使用 BCrypt 对密码进行哈希
        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        // 构建用户实体并插入
        AcUser user = new AcUser();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordHash);
        user.setEmail(dto.getEmail());
        user.setStatus(CommonConstants.IS_AVAILABLE);
        user.setIsDelete(CommonConstants.NOT_DELETE);
        acUserMapper.insert(user);
    }

    @Override
    public AcLoginVo login(AcUserLoginDTO dto) {
        // 根据用户名查询 ac_user
        LambdaQueryWrapper<AcUser> lqw = Wrappers.<AcUser>lambdaQuery()
            .eq(AcUser::getUsername, dto.getUsername());
        AcUser user = acUserMapper.selectOne(lqw);

        // 用户不存在
        if (user == null) {
            throw new ServiceException("用户名不存在");
        }

        // 账号停用
        if (CommonConstants.NOT_AVAILABLE.equals(user.getStatus())) {
            throw new ServiceException("账号已被停用");
        }

        // 对前端 Base64 编码的密码进行解码
        String rawPassword = new String(Base64.getDecoder().decode(dto.getPassword()));

        // BCrypt 校验密码
        if (!BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
            throw new ServiceException("密码错误");
        }

        // Sa-Token 登录，将用户名写入 Session
        // loginId 格式为 "ac_user:userId"，与框架 UserType 枚举匹配
        String loginId = "ac_user:" + user.getId();
        StpUserUtil.login(loginId);
        StpUserUtil.getTokenSession().set("userId", user.getId());
        StpUserUtil.getTokenSession().set("username", user.getUsername());

        // 构建返回对象
        AcLoginVo loginVo = new AcLoginVo();
        loginVo.setToken(StpUserUtil.getTokenValue());
        loginVo.setUser(BeanUtil.toBean(user, AcUserVo.class));
        return loginVo;
    }

    @Override
    public void logout() {
        try {
            StpUserUtil.logout();
        } catch (NotLoginException ignored) {
            // 幂等处理：未登录时登出不抛异常
        }
    }

    @Override
    public AcUserVo getLoginUserInfo() {
        // 获取当前登录用户 ID（loginId 格式为 "ac_user:userId"）
        String loginId = StpUserUtil.stpLogic.getLoginIdAsString();
        int userId = Integer.parseInt(loginId.substring(loginId.indexOf(":") + 1));
        AcUser user = acUserMapper.selectById(userId);
        return BeanUtil.toBean(user, AcUserVo.class);
    }
}

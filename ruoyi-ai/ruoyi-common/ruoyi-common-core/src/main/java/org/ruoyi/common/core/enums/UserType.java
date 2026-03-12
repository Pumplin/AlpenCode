package org.ruoyi.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ruoyi.common.core.utils.StringUtils;

/**
 * 设备类型
 * 针对多套 用户体系
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum UserType {

    /**
     * pc端
     */
    SYS_USER("sys_user"),

    /**
     * app端
     */
    APP_USER("app_user"),

    /**
     * OJ用户端
     */
    AC_USER("ac_user");

    private final String userType;

    public static UserType getUserType(String str) {
        // Sa-Token 默认 StpUtil 的 loginType 是 "login"，对应管理端 SYS_USER
        if ("login".equals(str)) {
            return SYS_USER;
        }
        for (UserType value : values()) {
            if (StringUtils.contains(str, value.getUserType())) {
                return value;
            }
        }
        throw new RuntimeException("'UserType' not found By " + str);
    }
}

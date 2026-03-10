package org.ruoyi.common.satoken.utils;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 用户端认证工具类（ac_user 专用）
 * <p>
 * 基于 Sa-Token 多账号体系，type = "ac_user"，
 * 与管理端 StpUtil（type="login"）完全隔离。
 * <p>
 * 使用 JWT 简单模式，与管理端保持一致。
 * Sa-Token 多账号体系下，不同 type 的 StpLogic 共享全局 SaTokenConfig，
 * 因此 token-name（Authorization）、token-prefix（Bearer）等配置自动继承。
 *
 * @author AlpenCode
 */
public class StpUserUtil {

    /**
     * 用户端账号体系标识
     */
    public static final String TYPE = "ac_user";

    /**
     * 用户端专属 StpLogic（JWT 简单模式）
     */
    public static StpLogic stpLogic = new StpLogicJwtForSimple(TYPE);

    private StpUserUtil() {
    }

    /**
     * 用户端登录
     *
     * @param id 用户ID
     */
    public static void login(Object id) {
        stpLogic.login(id);
    }

    /**
     * 用户端登出
     */
    public static void logout() {
        stpLogic.logout();
    }

    /**
     * 检查当前用户是否已登录，未登录则抛出 NotLoginException
     */
    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    /**
     * 获取当前登录用户ID（int 类型）
     *
     * @return 用户ID
     */
    public static int getLoginIdAsInt() {
        return stpLogic.getLoginIdAsInt();
    }

    /**
     * 获取当前 Token 值
     *
     * @return Token 字符串
     */
    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return true=已登录, false=未登录
     */
    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    /**
     * 获取当前 Token 对应的 Session
     *
     * @return SaSession
     */
    public static SaSession getTokenSession() {
        return stpLogic.getTokenSession();
    }
}

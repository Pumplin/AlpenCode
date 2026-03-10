package org.ruoyi.common.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.satoken.utils.StpUserUtil;
import org.ruoyi.common.security.config.properties.SecurityProperties;
import org.ruoyi.common.security.handler.AllUrlHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 权限安全配置
 *
 * @author Lion Li
 */

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;

    /**
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则
        registry.addInterceptor(new SaInterceptor(handler -> {
            AllUrlHandler allUrlHandler = SpringUtils.getBean(AllUrlHandler.class);

            // 用户端路由校验：/ac/** 下的受保护接口使用 StpUserUtil 校验
            // /oj/** 路径（如 /oj/problem, /oj/user 等）是管理端接口，走 StpUtil
            SaRouter.match("/ac/**")
                .notMatch(
                    "/ac/auth/login", "/ac/auth/register",
                    "/ac/problem/page", "/ac/problem/info/**", "/ac/problem/category/list"
                )
                .check(r -> StpUserUtil.checkLogin())
                .stop();

            // 管理端登录验证 -- 排除多个路径
            SaRouter
                // 获取所有的
                .match(allUrlHandler.getUrls())
                // 对未排除的路径进行检查
                .check(() -> {
                    // 检查是否登录 是否有token
                    StpUtil.checkLogin();
                });
        })).addPathPatterns("/**")
            // 排除不需要拦截的路径
            .excludePathPatterns(securityProperties.getExcludes());
    }

}

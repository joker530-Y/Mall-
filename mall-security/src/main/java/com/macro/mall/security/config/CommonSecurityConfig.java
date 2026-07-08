package com.macro.mall.security.config;

import com.macro.mall.security.component.*;
import com.macro.mall.security.util.JwtTokenUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SpringSecurity通用配置
 * 包括通用Bean、Security通用Bean及动态权限通用Bean
 * Created by macro on 2022/5/20.
 */
@Configuration
public class CommonSecurityConfig {

    /**
     * 注入BCrypt密码编码器
     * @return PasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 注入白名单URL配置
     * @return IgnoreUrlsConfig实例
     */
    @Bean
    public IgnoreUrlsConfig ignoreUrlsConfig() {
        return new IgnoreUrlsConfig();
    }

    /**
     * 注入JWT工具类
     * @return JwtTokenUtil实例
     */
    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil();
    }

    /**
     * 注入自定义权限拒绝处理器
     * @return RestfulAccessDeniedHandler实例
     */
    @Bean
    public RestfulAccessDeniedHandler restfulAccessDeniedHandler() {
        return new RestfulAccessDeniedHandler();
    }

    /**
     * 注入自定义认证入口点
     * @return RestAuthenticationEntryPoint实例
     */
    @Bean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    /**
     * 注入JWT认证过滤器
     * @return JwtAuthenticationTokenFilter实例
     */
    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(){
        return new JwtAuthenticationTokenFilter();
    }

    /**
     * 注入动态权限元数据源（仅在存在dynamicSecurityService Bean时才创建）
     * @return DynamicSecurityMetadataSource实例
     */
    @ConditionalOnBean(name = "dynamicSecurityService")
    @Bean
    public DynamicSecurityMetadataSource dynamicSecurityMetadataSource() {
        return new DynamicSecurityMetadataSource();
    }

    /**
     * 注入动态鉴权管理器（仅在存在dynamicSecurityService Bean时才创建）
     * @return DynamicAuthorizationManager实例
     */
    @ConditionalOnBean(name = "dynamicSecurityService")
    @Bean
    public DynamicAuthorizationManager dynamicAuthorizationManager() {
        return new DynamicAuthorizationManager();
    }
}

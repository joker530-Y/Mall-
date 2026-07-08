package com.macro.mall.security.config;

import com.macro.mall.security.component.DynamicAuthorizationManager;
import com.macro.mall.security.component.JwtAuthenticationTokenFilter;
import com.macro.mall.security.component.RestAuthenticationEntryPoint;
import com.macro.mall.security.component.RestfulAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * SpringSecurity相关配置，仅用于配置SecurityFilterChain
 * Created by macro on 2019/11/5.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 白名单URL配置 */
    @Autowired
    private IgnoreUrlsConfig ignoreUrlsConfig;
    /** 自定义权限拒绝处理器 */
    @Autowired
    private RestfulAccessDeniedHandler restfulAccessDeniedHandler;
    /** 自定义认证入口点 */
    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    /** JWT认证过滤器 */
    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    /** 动态鉴权管理器（可选） */
    @Autowired(required = false)
    private DynamicAuthorizationManager dynamicAuthorizationManager;

    /**
     * 配置SecurityFilterChain安全过滤链
     * @param httpSecurity HttpSecurity配置
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(registry -> {
            //不需要保护的资源路径允许访问
            for (String url : ignoreUrlsConfig.getUrls()) {
                registry.requestMatchers(url).permitAll();
            }
            //允许跨域请求的OPTIONS请求
            registry.requestMatchers(HttpMethod.OPTIONS).permitAll();
            //任何请求需要身份认证
        })
        //任何请求需要身份认证
        .authorizeHttpRequests(registry-> registry.anyRequest()
            //有动态权限配置时添加动态权限管理器
            .access(dynamicAuthorizationManager==null? AuthenticatedAuthorizationManager.authenticated():dynamicAuthorizationManager)
        )
        //关闭跨站请求防护
        .csrf(AbstractHttpConfigurer::disable)
        //修改Session生成策略为无状态会话
        .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        //自定义权限拒绝处理类
        .exceptionHandling(configurer -> configurer.accessDeniedHandler(restfulAccessDeniedHandler).authenticationEntryPoint(restAuthenticationEntryPoint))
        //自定义权限拦截器JWT过滤器
        .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

}

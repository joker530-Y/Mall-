package com.macro.mall.config;

import com.macro.mall.model.UmsResource;
import com.macro.mall.security.component.DynamicSecurityService;
import com.macro.mall.service.UmsAdminService;
import com.macro.mall.service.UmsResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mall-security模块相关配置
 * Created by macro on 2019/11/9.
 */
@Configuration
public class MallSecurityConfig {

    /** 后台用户管理服务 */
    @Autowired
    private UmsAdminService adminService;
    /** 后台资源管理服务 */
    @Autowired
    private UmsResourceService resourceService;

    /**
     * 注入SpringSecurity用户详情服务
     * @return UserDetailsService实例
     */
    @Bean
    public UserDetailsService userDetailsService() {
        //获取登录用户信息
        return username -> adminService.loadUserByUsername(username);
    }

    /**
     * 注入动态权限服务，加载所有资源URL与权限的映射关系
     * @return DynamicSecurityService实例
     */
    @Bean
    public DynamicSecurityService dynamicSecurityService() {
        return new DynamicSecurityService() {
            @Override
            public Map<String, ConfigAttribute> loadDataSource() {
                Map<String, ConfigAttribute> map = new ConcurrentHashMap<>();
                List<UmsResource> resourceList = resourceService.listAll();
                for (UmsResource resource : resourceList) {
                    map.put(resource.getUrl(), new org.springframework.security.access.SecurityConfig(resource.getId() + ":" + resource.getName()));
                }
                return map;
            }
        };
    }
}

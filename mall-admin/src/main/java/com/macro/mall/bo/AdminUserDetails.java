package com.macro.mall.bo;

import com.macro.mall.model.UmsAdmin;
import com.macro.mall.model.UmsResource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpringSecurity需要的用户信息封装类
 * Created by macro on 2018/4/26.
 */
public class AdminUserDetails implements UserDetails {
    /** 后台用户信息 */
    private final UmsAdmin umsAdmin;
    /** 用户拥有的资源列表 */
    private final List<UmsResource> resourceList;

    /**
     * 构造方法
     * @param umsAdmin 后台用户
     * @param resourceList 资源列表
     */
    public AdminUserDetails(UmsAdmin umsAdmin,List<UmsResource> resourceList) {
        this.umsAdmin = umsAdmin;
        this.resourceList = resourceList;
    }

    /**
     * 获取用户权限列表，将资源列表转换为GrantedAuthority集合
     * @return 权限集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return resourceList.stream()
                .map(resource ->new SimpleGrantedAuthority(resource.getId()+":"+resource.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return umsAdmin.getPassword();
    }

    @Override
    public String getUsername() {
        return umsAdmin.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 判断账户是否已启用
     * @return 状态为1表示启用
     */
    @Override
    public boolean isEnabled() {
        return umsAdmin.getStatus().equals(1);
    }
}

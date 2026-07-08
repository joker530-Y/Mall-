package com.macro.mall.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.macro.mall.bo.AdminUserDetails;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.common.util.RequestUtil;
import com.macro.mall.dao.UmsAdminRoleRelationDao;
import com.macro.mall.dto.UmsAdminParam;
import com.macro.mall.dto.UpdateAdminPasswordParam;
import com.macro.mall.mapper.UmsAdminLoginLogMapper;
import com.macro.mall.mapper.UmsAdminMapper;
import com.macro.mall.mapper.UmsAdminRoleRelationMapper;
import com.macro.mall.model.*;
import com.macro.mall.security.util.JwtTokenUtil;
import com.macro.mall.security.util.SpringUtil;
import com.macro.mall.service.UmsAdminCacheService;
import com.macro.mall.service.UmsAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 后台用户管理Service实现类
 * Created by macro on 2018/4/26.
 */
@Service
public class UmsAdminServiceImpl implements UmsAdminService {
    /** 日志记录器 */
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAdminServiceImpl.class);
    /** JWT工具类 */
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    /** 密码编码器 */
    @Autowired
    private PasswordEncoder passwordEncoder;
    /** 后台用户Mapper */
    @Autowired
    private UmsAdminMapper adminMapper;
    /** 用户角色关系Mapper */
    @Autowired
    private UmsAdminRoleRelationMapper adminRoleRelationMapper;
    /** 用户角色关系Dao */
    @Autowired
    private UmsAdminRoleRelationDao adminRoleRelationDao;
    /** 登录日志Mapper */
    @Autowired
    private UmsAdminLoginLogMapper loginLogMapper;

    /**
     * 根据用户名获取后台管理员，先从缓存获取，缓存没有则从数据库获取并写入缓存
     * @param username 用户名
     * @return 后台管理员信息
     */
    @Override
    public UmsAdmin getAdminByUsername(String username) {
        //先从缓存中获取数据
        UmsAdmin admin = getCacheService().getAdmin(username);
        if (admin != null) return admin;
        //缓存中没有从数据库中获取
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UmsAdmin> adminList = adminMapper.selectByExample(example);
        if (adminList != null && adminList.size() > 0) {
            admin = adminList.get(0);
            //将数据库中的数据存入缓存中
            getCacheService().setAdmin(admin);
            return admin;
        }
        return null;
    }

    /**
     * 用户注册，检查用户名唯一性并对密码进行BCrypt加密
     * @param umsAdminParam 用户注册参数
     * @return 注册成功返回用户信息，重复返回null
     */
    @Override
    public UmsAdmin register(UmsAdminParam umsAdminParam) {
        UmsAdmin umsAdmin = new UmsAdmin();
        BeanUtils.copyProperties(umsAdminParam, umsAdmin);
        umsAdmin.setCreateTime(new Date());
        umsAdmin.setStatus(1);
        //查询是否有相同用户名的用户
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(umsAdmin.getUsername());
        List<UmsAdmin> umsAdminList = adminMapper.selectByExample(example);
        if (umsAdminList.size() > 0) {
            return null;
        }
        //将密码进行加密操作
        String encodePassword = passwordEncoder.encode(umsAdmin.getPassword());
        umsAdmin.setPassword(encodePassword);
        adminMapper.insert(umsAdmin);
        return umsAdmin;
    }

    /**
     * 用户登录，验证密码和账户状态后生成JWT token
     * @param username 用户名
     * @param password 密码（客户端已加密）
     * @return JWT token字符串
     */
    @Override
    public String login(String username, String password) {
        String token = null;
        //密码需要客户端加密后传递
        try {
            UserDetails userDetails = loadUserByUsername(username);
            if(!passwordEncoder.matches(password,userDetails.getPassword())){
                Asserts.fail("密码不正确");
            }
            if(!userDetails.isEnabled()){
                Asserts.fail("帐号已被禁用");
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            token = jwtTokenUtil.generateToken(userDetails);
//            updateLoginTimeByUsername(username);
            insertLoginLog(username);
        } catch (AuthenticationException e) {
            LOGGER.warn("登录异常:{}", e.getMessage());
        }
        return token;
    }

    /**
     * 添加登录记录
     * @param username 用户名
     */
    private void insertLoginLog(String username) {
        UmsAdmin admin = getAdminByUsername(username);
        if(admin==null) return;
        UmsAdminLoginLog loginLog = new UmsAdminLoginLog();
        loginLog.setAdminId(admin.getId());
        loginLog.setCreateTime(new Date());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        loginLog.setIp(RequestUtil.getRequestIp(request));
        loginLogMapper.insert(loginLog);
    }

    /**
     * 根据用户名修改登录时间
     */
    private void updateLoginTimeByUsername(String username) {
        UmsAdmin record = new UmsAdmin();
        record.setLoginTime(new Date());
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(username);
        adminMapper.updateByExampleSelective(record, example);
    }

    /**
     * 刷新JWT token
     * @param oldToken 带tokenHead的旧token
     * @return 新的token字符串
     */
    @Override
    public String refreshToken(String oldToken) {
        return jwtTokenUtil.refreshHeadToken(oldToken);
    }

    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UmsAdmin getItem(Long id) {
        return adminMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据用户名或昵称分页查询用户列表
     * @param keyword 搜索关键词
     * @param pageSize 每页数量
     * @param pageNum 页码
     * @return 用户列表
     */
    @Override
    public List<UmsAdmin> list(String keyword, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        UmsAdminExample example = new UmsAdminExample();
        UmsAdminExample.Criteria criteria = example.createCriteria();
        if (!StrUtil.isEmpty(keyword)) {
            criteria.andUsernameLike("%" + keyword + "%");
            example.or(example.createCriteria().andNickNameLike("%" + keyword + "%"));
        }
        return adminMapper.selectByExample(example);
    }

    /**
     * 修改用户信息，处理密码加密逻辑
     * @param id 用户ID
     * @param admin 用户信息
     * @return 影响行数
     */
    @Override
    public int update(Long id, UmsAdmin admin) {
        admin.setId(id);
        UmsAdmin rawAdmin = adminMapper.selectByPrimaryKey(id);
        if(rawAdmin.getPassword().equals(admin.getPassword())){
            //与原加密密码相同的不需要修改
            admin.setPassword(null);
        }else{
            //与原加密密码不同的需要加密修改
            if(StrUtil.isEmpty(admin.getPassword())){
                admin.setPassword(null);
            }else{
                admin.setPassword(passwordEncoder.encode(admin.getPassword()));
            }
        }
        int count = adminMapper.updateByPrimaryKeySelective(admin);
        getCacheService().delAdmin(id);
        return count;
    }

    /**
     * 删除用户并清空缓存
     * @param id 用户ID
     * @return 影响行数
     */
    @Override
    public int delete(Long id) {
        int count = adminMapper.deleteByPrimaryKey(id);
        getCacheService().delAdmin(id);
        getCacheService().delResourceList(id);
        return count;
    }

    /**
     * 更新用户角色关系，先删后增
     * @param adminId 管理员ID
     * @param roleIds 角色ID列表
     * @return 分配的角色数量
     */
    @Override
    public int updateRole(Long adminId, List<Long> roleIds) {
        int count = roleIds == null ? 0 : roleIds.size();
        //先删除原来的关系
        UmsAdminRoleRelationExample adminRoleRelationExample = new UmsAdminRoleRelationExample();
        adminRoleRelationExample.createCriteria().andAdminIdEqualTo(adminId);
        adminRoleRelationMapper.deleteByExample(adminRoleRelationExample);
        //建立新关系
        if (!CollectionUtils.isEmpty(roleIds)) {
            List<UmsAdminRoleRelation> list = new ArrayList<>();
            for (Long roleId : roleIds) {
                UmsAdminRoleRelation roleRelation = new UmsAdminRoleRelation();
                roleRelation.setAdminId(adminId);
                roleRelation.setRoleId(roleId);
                list.add(roleRelation);
            }
            adminRoleRelationDao.insertList(list);
        }
        getCacheService().delResourceList(adminId);
        return count;
    }

    /**
     * 获取用户的角色列表
     * @param adminId 管理员ID
     * @return 角色列表
     */
    @Override
    public List<UmsRole> getRoleList(Long adminId) {
        return adminRoleRelationDao.getRoleList(adminId);
    }

    /**
     * 获取用户的资源权限列表，先从缓存获取
     * @param adminId 管理员ID
     * @return 资源权限列表
     */
    @Override
    public List<UmsResource> getResourceList(Long adminId) {
        //先从缓存中获取数据
        List<UmsResource> resourceList = getCacheService().getResourceList(adminId);
        if(CollUtil.isNotEmpty(resourceList)){
            return  resourceList;
        }
        //缓存中没有从数据库中获取
        resourceList = adminRoleRelationDao.getResourceList(adminId);
        if(CollUtil.isNotEmpty(resourceList)){
            //将数据库中的数据存入缓存中
            getCacheService().setResourceList(adminId,resourceList);
        }
        return resourceList;
    }

    /**
     * 修改密码，验证旧密码正确后更新
     * @param param 密码修改参数
     * @return 1成功 -1参数为空 -2用户不存在 -3旧密码错误
     */
    @Override
    public int updatePassword(UpdateAdminPasswordParam param) {
        if(StrUtil.isEmpty(param.getUsername())
                ||StrUtil.isEmpty(param.getOldPassword())
                ||StrUtil.isEmpty(param.getNewPassword())){
            return -1;
        }
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(param.getUsername());
        List<UmsAdmin> adminList = adminMapper.selectByExample(example);
        if(CollUtil.isEmpty(adminList)){
            return -2;
        }
        UmsAdmin umsAdmin = adminList.get(0);
        if(!passwordEncoder.matches(param.getOldPassword(),umsAdmin.getPassword())){
            return -3;
        }
        umsAdmin.setPassword(passwordEncoder.encode(param.getNewPassword()));
        adminMapper.updateByPrimaryKey(umsAdmin);
        getCacheService().delAdmin(umsAdmin.getId());
        return 1;
    }

    /**
     * 加载用户详情用于SpringSecurity认证
     * @param username 用户名
     * @return UserDetails
     */
    @Override
    public UserDetails loadUserByUsername(String username){
        //获取用户信息
        UmsAdmin admin = getAdminByUsername(username);
        if (admin != null) {
            List<UmsResource> resourceList = getResourceList(admin.getId());
            return new AdminUserDetails(admin,resourceList);
        }
        throw new UsernameNotFoundException("用户名或密码错误");
    }

    /**
     * 获取缓存服务Bean
     * @return UmsAdminCacheService实例
     */
    @Override
    public UmsAdminCacheService getCacheService() {
        return SpringUtil.getBean(UmsAdminCacheService.class);
    }

    /**
     * 用户登出，清空缓存中的用户和权限数据
     * @param username 用户名
     */
    @Override
    public void logout(String username) {
        //清空缓存中的用户相关数据
        UmsAdmin admin = getCacheService().getAdmin(username);
        getCacheService().delAdmin(admin.getId());
        getCacheService().delResourceList(admin.getId());
    }
}

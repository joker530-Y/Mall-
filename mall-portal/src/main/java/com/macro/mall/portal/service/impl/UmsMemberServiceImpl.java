package com.macro.mall.portal.service.impl;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mapper.UmsMemberLevelMapper;
import com.macro.mall.mapper.UmsMemberMapper;
import com.macro.mall.model.UmsMember;
import com.macro.mall.model.UmsMemberExample;
import com.macro.mall.model.UmsMemberLevel;
import com.macro.mall.model.UmsMemberLevelExample;
import com.macro.mall.portal.domain.MemberDetails;
import com.macro.mall.portal.service.UmsMemberCacheService;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.security.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 会员管理Service实现类
 * Created by macro on 2018/8/3.
 */
@Service
public class UmsMemberServiceImpl implements UmsMemberService {
    /** 日志记录器 */
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsMemberServiceImpl.class);
    /** 密码编码器 */
    @Autowired
    private PasswordEncoder passwordEncoder;
    /** JWT工具类 */
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    /** 会员Mapper */
    @Autowired
    private UmsMemberMapper memberMapper;
    /** 会员等级Mapper */
    @Autowired
    private UmsMemberLevelMapper memberLevelMapper;
    /** 会员缓存服务 */
    @Autowired
    private UmsMemberCacheService memberCacheService;
    /** Redis中验证码key前缀 */
    @Value("${redis.key.authCode}")
    private String REDIS_KEY_PREFIX_AUTH_CODE;
    /** 验证码过期时间（秒） */
    @Value("${redis.expire.authCode}")
    private Long AUTH_CODE_EXPIRE_SECONDS;

    /**
     * 根据用户名获取会员信息，先从缓存获取
     * @param username 用户名
     * @return 会员信息
     */
    @Override
    public UmsMember getByUsername(String username) {
        UmsMember member = memberCacheService.getMember(username);
        if(member!=null) return member;
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UmsMember> memberList = memberMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(memberList)) {
            member = memberList.get(0);
            memberCacheService.setMember(member);
            return member;
        }
        return null;
    }

    /**
     * 根据用户ID获取会员信息
     * @param id 用户ID
     * @return 会员信息
     */
    @Override
    public UmsMember getById(Long id) {
        return memberMapper.selectByPrimaryKey(id);
    }

    /**
     * 会员注册，验证验证码和用户名/手机唯一性，设置默认会员等级
     * @param username 用户名
     * @param password 密码
     * @param telephone 手机号
     * @param authCode 短信验证码
     */
    @Override
    public void register(String username, String password, String telephone, String authCode) {
        //验证验证码
        if(!verifyAuthCode(authCode,telephone)){
            Asserts.fail("验证码错误");
        }
        //查询是否已有该用户
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andUsernameEqualTo(username);
        example.or(example.createCriteria().andPhoneEqualTo(telephone));
        List<UmsMember> umsMembers = memberMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(umsMembers)) {
            Asserts.fail("该用户已经存在");
        }
        //没有该用户进行添加操作
        UmsMember umsMember = new UmsMember();
        umsMember.setUsername(username);
        umsMember.setPhone(telephone);
        umsMember.setPassword(passwordEncoder.encode(password));
        umsMember.setCreateTime(new Date());
        umsMember.setStatus(1);
        //获取默认会员等级并设置
        UmsMemberLevelExample levelExample = new UmsMemberLevelExample();
        levelExample.createCriteria().andDefaultStatusEqualTo(1);
        List<UmsMemberLevel> memberLevelList = memberLevelMapper.selectByExample(levelExample);
        if (!CollectionUtils.isEmpty(memberLevelList)) {
            umsMember.setMemberLevelId(memberLevelList.get(0).getId());
        }
        memberMapper.insert(umsMember);
        umsMember.setPassword(null);
    }

    /**
     * 生成6位数字验证码并存入缓存
     * @param telephone 手机号
     * @return 验证码
     */
    @Override
    public String generateAuthCode(String telephone) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i=0;i<6;i++){
            sb.append(random.nextInt(10));
        }
        memberCacheService.setAuthCode(telephone,sb.toString());
        return sb.toString();
    }

    /**
     * 通过手机号和验证码修改密码
     * @param telephone 手机号
     * @param password 新密码
     * @param authCode 验证码
     */
    @Override
    public void updatePassword(String telephone, String password, String authCode) {
        UmsMemberExample example = new UmsMemberExample();
        example.createCriteria().andPhoneEqualTo(telephone);
        List<UmsMember> memberList = memberMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(memberList)){
            Asserts.fail("该账号不存在");
        }
        //验证验证码
        if(!verifyAuthCode(authCode,telephone)){
            Asserts.fail("验证码错误");
        }
        UmsMember umsMember = memberList.get(0);
        umsMember.setPassword(passwordEncoder.encode(password));
        memberMapper.updateByPrimaryKeySelective(umsMember);
        memberCacheService.delMember(umsMember.getId());
    }

    /**
     * 从SpringSecurity上下文中获取当前登录会员信息
     * @return 当前会员
     */
    @Override
    public UmsMember getCurrentMember() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        Authentication auth = ctx.getAuthentication();
        MemberDetails memberDetails = (MemberDetails) auth.getPrincipal();
        return memberDetails.getUmsMember();
    }

    /**
     * 更新会员积分并清除缓存
     * @param id 会员ID
     * @param integration 新积分值
     */
    @Override
    public void updateIntegration(Long id, Integer integration) {
        UmsMember record=new UmsMember();
        record.setId(id);
        record.setIntegration(integration);
        memberMapper.updateByPrimaryKeySelective(record);
        memberCacheService.delMember(id);
    }

    /**
     * 加载会员详情用于SpringSecurity认证
     * @param username 用户名
     * @return UserDetails
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        UmsMember member = getByUsername(username);
        if(member!=null){
            return new MemberDetails(member);
        }
        throw new UsernameNotFoundException("用户名或密码错误");
    }

    /**
     * 会员登录，验证密码后生成JWT token
     * @param username 用户名
     * @param password 密码（客户端已加密）
     * @return JWT token
     */
    @Override
    public String login(String username, String password) {
        String token = null;
        //密码需要客户端加密后传递
        try {
            UserDetails userDetails = loadUserByUsername(username);
            if(!passwordEncoder.matches(password,userDetails.getPassword())){
                throw new BadCredentialsException("密码不正确");
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            token = jwtTokenUtil.generateToken(userDetails);
        } catch (AuthenticationException e) {
            LOGGER.warn("登录异常:{}", e.getMessage());
        }
        return token;
    }

    /**
     * 刷新JWT token
     * @param token 带前缀的旧token
     * @return 新token
     */
    @Override
    public String refreshToken(String token) {
        return jwtTokenUtil.refreshHeadToken(token);
    }

    /**
     * 验证短信验证码是否匹配
     * @param authCode 输入的验证码
     * @param telephone 手机号
     * @return true表示验证通过
     */
    private boolean verifyAuthCode(String authCode, String telephone){
        if(StrUtil.isEmpty(authCode)){
            return false;
        }
        String realAuthCode = memberCacheService.getAuthCode(telephone);
        return authCode.equals(realAuthCode);
    }

}

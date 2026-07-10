package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 会员公开信息（不含密码等敏感字段）
 */
@Getter
@Setter
public class MemberInfo {
    @Schema(title = "会员ID")
    private Long id;
    @Schema(title = "用户名")
    private String username;
    @Schema(title = "昵称")
    private String nickname;
    @Schema(title = "手机号码")
    private String phone;
    @Schema(title = "头像")
    private String icon;
    @Schema(title = "性别")
    private Integer gender;
    @Schema(title = "生日")
    private Date birthday;
    @Schema(title = "所在城市")
    private String city;
    @Schema(title = "职业")
    private String job;
    @Schema(title = "个性签名")
    private String personalizedSignature;
    @Schema(title = "积分")
    private Integer integration;
    @Schema(title = "成长值")
    private Integer growth;
    @Schema(title = "剩余抽奖次数")
    private Integer luckeyCount;
    @Schema(title = "历史积分数量")
    private Integer historyIntegration;
}

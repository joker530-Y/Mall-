package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AiCustomerChatParam {
    @Schema(title = "用户问题")
    private String question;

    @Schema(title = "可选订单号，仅查询当前登录会员自己的订单")
    private String orderSn;

    @Schema(title = "可选商品ID，用于精确召回商品知识")
    private Long productId;

    @Schema(title = "最多召回的知识片段数")
    private Integer maxContextCount;
}

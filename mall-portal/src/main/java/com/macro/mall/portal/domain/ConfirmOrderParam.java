package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 生成确认单时传入的参数
 */
@Data
public class ConfirmOrderParam {
    @Schema(title = "被选中的购物车商品ID")
    private List<Long> cartIds;
    @Schema(title = "优惠券ID（试算）")
    private Long couponId;
    @Schema(title = "使用的积分数（试算）")
    private Integer useIntegration;
}

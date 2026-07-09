package com.macro.mall.portal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode
public class BaselineSeckillOrderResult {
    private Long orderId;
    private String orderSn;
    private Long relationId;
    private Long productId;
    private Long productSkuId;
    private Integer quantity;
    private BigDecimal payAmount;
    private Integer remainingStock;
}

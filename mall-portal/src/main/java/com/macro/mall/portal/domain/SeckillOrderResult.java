package com.macro.mall.portal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class SeckillOrderResult {
    private String requestId;
    private Long relationId;
    private Long orderId;
    private String orderSn;
    private String status;
    private String reason;
}

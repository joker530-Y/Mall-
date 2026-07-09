package com.macro.mall.portal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class SeckillOrderSubmitResult {
    private String requestId;
    private Long relationId;
    private String status;
    private Integer remainingStock;
}

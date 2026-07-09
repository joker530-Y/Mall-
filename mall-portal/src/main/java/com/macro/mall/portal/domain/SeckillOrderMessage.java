package com.macro.mall.portal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
public class SeckillOrderMessage implements Serializable {
    private String requestId;
    private Long relationId;
    private Long memberId;
    private Long productSkuId;
    private Long memberReceiveAddressId;
    private Integer quantity;
    private Integer payType;
    private static final long serialVersionUID = 1L;
}

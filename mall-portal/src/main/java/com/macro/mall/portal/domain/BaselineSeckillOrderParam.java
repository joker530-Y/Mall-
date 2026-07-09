package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class BaselineSeckillOrderParam {
    @Schema(title = "Flash promotion product relation ID")
    private Long relationId;

    @Schema(title = "Product SKU ID")
    private Long productSkuId;

    @Schema(title = "Member receive address ID")
    private Long memberReceiveAddressId;

    @Schema(title = "Order quantity")
    private Integer quantity;

    @Schema(title = "Pay type")
    private Integer payType;
}

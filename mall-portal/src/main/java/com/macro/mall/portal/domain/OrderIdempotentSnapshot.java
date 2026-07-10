package com.macro.mall.portal.domain;

import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderItem;
import lombok.Data;

import java.util.List;

/**
 * 下单幂等结果快照，用于 Redis 缓存重复请求响应
 */
@Data
public class OrderIdempotentSnapshot {
    private OmsOrder order;
    private List<OmsOrderItem> orderItemList;
}

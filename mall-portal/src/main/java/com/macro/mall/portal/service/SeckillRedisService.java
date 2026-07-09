package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.SeckillOrderMessage;
import com.macro.mall.portal.domain.SeckillOrderParam;
import com.macro.mall.portal.domain.SeckillOrderResult;
import com.macro.mall.portal.domain.SeckillOrderSubmitResult;
import com.macro.mall.portal.domain.SeckillWarmupResult;

public interface SeckillRedisService {
    SeckillWarmupResult warmup(Long relationId);

    SeckillOrderSubmitResult submitOrder(SeckillOrderParam orderParam);

    SeckillOrderResult getResult(Long relationId);

    void consumeOrder(SeckillOrderMessage message);

    void markConsumeFailed(SeckillOrderMessage message, String reason);
}

package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.BaselineSeckillOrderParam;
import com.macro.mall.portal.domain.BaselineSeckillOrderResult;

public interface SeckillBaselineService {
    BaselineSeckillOrderResult generateOrder(BaselineSeckillOrderParam orderParam);
}

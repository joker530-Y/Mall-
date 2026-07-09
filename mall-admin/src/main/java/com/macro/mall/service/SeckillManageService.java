package com.macro.mall.service;

import com.macro.mall.dto.SeckillManageSummary;
import com.macro.mall.dto.SeckillManageWarmupResult;
import com.macro.mall.dto.SeckillOrderLogItem;

import java.util.List;

public interface SeckillManageService {
    SeckillManageWarmupResult warmup(Long relationId);

    SeckillManageSummary summary(Long relationId);

    List<SeckillOrderLogItem> listOrderLogs(Long relationId, Integer pageSize, Integer pageNum);

    long countOrderLogs(Long relationId);
}

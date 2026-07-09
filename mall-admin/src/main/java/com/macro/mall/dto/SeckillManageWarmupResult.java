package com.macro.mall.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class SeckillManageWarmupResult {
    private Long relationId;
    private Integer stock;
    private Integer limit;
    private String stockKey;
    private String warmedAt;
}

package com.macro.mall.portal.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class SeckillWarmupResult {
    private Long relationId;
    private Integer stock;
    private Integer limit;
    private String stockKey;
    private String warmedAt;
}

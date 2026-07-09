package com.macro.mall.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class SeckillManageSummary {
    private Long relationId;
    private Long productId;
    private Long flashPromotionId;
    private Long flashPromotionSessionId;
    private Integer dbRemainingStock;
    private Integer redisStock;
    private Integer limit;
    private Integer processingCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer totalRequestCount;
    private Integer duplicateMemberCount;
    private Integer oversoldCount;
    private String stockKey;
    private String refreshedAt;
}

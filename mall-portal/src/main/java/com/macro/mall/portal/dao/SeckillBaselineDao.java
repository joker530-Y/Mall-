package com.macro.mall.portal.dao;

import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface SeckillBaselineDao {
    int insertOrderLog(@Param("memberId") Long memberId,
                       @Param("relationId") Long relationId,
                       @Param("requestId") String requestId,
                       @Param("createdAt") Date createdAt);

    int updateOrderLogSuccess(@Param("memberId") Long memberId,
                              @Param("relationId") Long relationId,
                              @Param("orderId") Long orderId,
                              @Param("updatedAt") Date updatedAt);

    int decreaseFlashPromotionStock(@Param("relationId") Long relationId,
                                    @Param("quantity") Integer quantity);

    Integer getRemainingStock(@Param("relationId") Long relationId);
}

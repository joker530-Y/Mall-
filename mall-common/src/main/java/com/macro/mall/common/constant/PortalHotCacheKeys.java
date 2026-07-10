package com.macro.mall.common.constant;

/**
 * 门户热点缓存逻辑 key（不含 redis.database 与 hot 前缀）。
 * 实际 Redis key = {redis.database}:hot:{logicalKey}
 */
public final class PortalHotCacheKeys {
    public static final String PRODUCT_DETAIL_PREFIX = "product:detail:";
    public static final String PRODUCT_CATEGORY_TREE = "product:category-tree";
    public static final String HOME_CONTENT_PREFIX = "home:content:";
    public static final String HOME_RECOMMEND_PREFIX = "home:recommend:";
    public static final String HOME_HOT_PREFIX = "home:hot:";
    public static final String HOME_NEW_PREFIX = "home:new:";
    public static final String HOME_PREFIX = "home:";

    private PortalHotCacheKeys() {
    }

    public static String productDetail(Long productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }
}

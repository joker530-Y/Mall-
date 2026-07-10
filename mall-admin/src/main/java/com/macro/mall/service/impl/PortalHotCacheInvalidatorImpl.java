package com.macro.mall.service.impl;

import com.macro.mall.common.constant.PortalHotCacheKeys;
import com.macro.mall.common.service.RedisService;
import com.macro.mall.service.PortalHotCacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * 通过删除 Redis 热点 key 失效门户缓存。
 * 门户本地 Caffeine 最长约 30s 过期；多实例下以 Redis 层为准。
 */
@Service
public class PortalHotCacheInvalidatorImpl implements PortalHotCacheInvalidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortalHotCacheInvalidatorImpl.class);

    @Autowired
    private RedisService redisService;
    @Value("${redis.database}")
    private String redisDatabase;

    @Override
    public void invalidateProduct(Long productId) {
        if (productId != null) {
            safeDel(hotKey(PortalHotCacheKeys.productDetail(productId)));
        }
        invalidateHomeCatalog();
    }

    @Override
    public void invalidateProducts(Collection<Long> productIds) {
        if (!CollectionUtils.isEmpty(productIds)) {
            for (Long productId : productIds) {
                if (productId != null) {
                    safeDel(hotKey(PortalHotCacheKeys.productDetail(productId)));
                }
            }
        }
        invalidateHomeCatalog();
    }

    @Override
    public void invalidateHomeCatalog() {
        safeDelByPattern(hotKey(PortalHotCacheKeys.HOME_PREFIX) + "*");
        invalidateCategoryTree();
    }

    @Override
    public void invalidateCategoryTree() {
        safeDel(hotKey(PortalHotCacheKeys.PRODUCT_CATEGORY_TREE));
    }

    private String hotKey(String logicalKey) {
        return redisDatabase + ":hot:" + logicalKey;
    }

    private void safeDel(String key) {
        try {
            redisService.del(key);
        } catch (Exception e) {
            LOGGER.warn("invalidate portal hot cache failed, key={}", key, e);
        }
    }

    private void safeDelByPattern(String pattern) {
        try {
            redisService.delByPattern(pattern);
        } catch (Exception e) {
            LOGGER.warn("invalidate portal hot cache by pattern failed, pattern={}", pattern, e);
        }
    }
}

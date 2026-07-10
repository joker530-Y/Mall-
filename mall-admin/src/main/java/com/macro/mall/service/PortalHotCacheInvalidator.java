package com.macro.mall.service;

import java.util.Collection;

/**
 * 后台写操作后失效门户热点缓存（与 portal 共用 Redis key 约定）。
 */
public interface PortalHotCacheInvalidator {

    void invalidateProduct(Long productId);

    void invalidateProducts(Collection<Long> productIds);

    void invalidateHomeCatalog();

    void invalidateCategoryTree();
}

package com.macro.mall.service.impl;

import com.macro.mall.common.constant.PortalHotCacheKeys;
import com.macro.mall.common.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortalHotCacheInvalidatorImplTest {

    @InjectMocks
    private PortalHotCacheInvalidatorImpl invalidator;

    @Mock
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invalidator, "redisDatabase", "mall");
    }

    @Test
    void invalidateProduct_shouldDeleteDetailAndHomeKeys() {
        invalidator.invalidateProduct(26L);

        verify(redisService).del(eq("mall:hot:" + PortalHotCacheKeys.productDetail(26L)));
        verify(redisService).delByPattern(eq("mall:hot:home:*"));
        verify(redisService).del(eq("mall:hot:" + PortalHotCacheKeys.PRODUCT_CATEGORY_TREE));
    }

    @Test
    void invalidateProducts_shouldDeleteEachDetail() {
        invalidator.invalidateProducts(List.of(1L, 2L));

        verify(redisService).del(eq("mall:hot:" + PortalHotCacheKeys.productDetail(1L)));
        verify(redisService).del(eq("mall:hot:" + PortalHotCacheKeys.productDetail(2L)));
        verify(redisService).delByPattern(eq("mall:hot:home:*"));
    }
}

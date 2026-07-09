package com.macro.mall.portal.component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.macro.mall.common.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Two-level cache for read-heavy portal data.
 */
@Component
public class HotDataCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotDataCache.class);
    private static final String NULL_VALUE = "__MALL_HOT_CACHE_NULL__";
    private static final long NULL_VALUE_TTL_SECONDS = 60L;
    private static final long LOCK_TTL_SECONDS = 10L;

    private final Cache<String, Object> localCache = Caffeine.newBuilder()
            .maximumSize(2_000)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    @Autowired
    private RedisService redisService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Value("${redis.database}")
    private String redisDatabase;
    @Value("${redis.expire.hot-data:300}")
    private Long hotDataExpire;
    @Value("${redis.expire.hot-data-random-bound:120}")
    private Long randomExpireBound;

    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        String cacheKey = buildKey(key);
        Object localValue = localCache.getIfPresent(cacheKey);
        T localResult = castCacheValue(localValue, type);
        if (localValue != null && (localResult != null || isNullValue(localValue))) {
            return localResult;
        }

        Object redisValue = getRedisValue(cacheKey);
        T redisResult = castCacheValue(redisValue, type);
        if (redisValue != null && (redisResult != null || isNullValue(redisValue))) {
            localCache.put(cacheKey, redisValue);
            return redisResult;
        }

        String lockKey = cacheKey + ":lock";
        Boolean locked = tryLock(lockKey);
        if (Boolean.TRUE.equals(locked)) {
            try {
                T loaded = loader.get();
                put(cacheKey, loaded);
                return loaded;
            } finally {
                delRedisValue(lockKey);
            }
        }

        sleepBriefly();
        Object retryValue = getRedisValue(cacheKey);
        T retryResult = castCacheValue(retryValue, type);
        if (retryValue != null && (retryResult != null || isNullValue(retryValue))) {
            localCache.put(cacheKey, retryValue);
            return retryResult;
        }
        T loaded = loader.get();
        put(cacheKey, loaded);
        return loaded;
    }

    public void evict(String key) {
        String cacheKey = buildKey(key);
        localCache.invalidate(cacheKey);
        delRedisValue(cacheKey);
    }

    public void evictLocal(String key) {
        localCache.invalidate(buildKey(key));
    }

    private void put(String cacheKey, Object value) {
        Object cacheValue = Objects.requireNonNullElse(value, NULL_VALUE);
        localCache.put(cacheKey, cacheValue);
        long ttl = value == null ? NULL_VALUE_TTL_SECONDS : randomTtl();
        setRedisValue(cacheKey, cacheValue, ttl);
    }

    private long randomTtl() {
        long bound = Math.max(0L, randomExpireBound);
        return hotDataExpire + (bound == 0L ? 0L : ThreadLocalRandom.current().nextLong(bound + 1));
    }

    private String buildKey(String key) {
        return redisDatabase + ":hot:" + key;
    }

    private boolean isNullValue(Object value) {
        return NULL_VALUE.equals(value);
    }

    private Object getRedisValue(String cacheKey) {
        try {
            return redisService.get(cacheKey);
        } catch (Exception e) {
            LOGGER.warn("Read hot cache from Redis failed, key={}", cacheKey, e);
            return null;
        }
    }

    private void setRedisValue(String cacheKey, Object value, long ttl) {
        try {
            redisService.set(cacheKey, value, ttl);
        } catch (Exception e) {
            LOGGER.warn("Write hot cache to Redis failed, key={}", cacheKey, e);
        }
    }

    private void delRedisValue(String cacheKey) {
        try {
            redisService.del(cacheKey);
        } catch (Exception e) {
            LOGGER.warn("Delete hot cache from Redis failed, key={}", cacheKey, e);
        }
    }

    private Boolean tryLock(String lockKey) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.warn("Acquire hot cache rebuild lock failed, key={}", lockKey, e);
            return false;
        }
    }

    private <T> T castCacheValue(Object value, Class<T> type) {
        if (value == null || isNullValue(value)) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    private void sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

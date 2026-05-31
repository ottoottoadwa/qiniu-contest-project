package com.prreview.infrastructure.cache;

import com.prreview.domain.port.out.CachePort;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Implements CachePort using Caffeine in-process cache.
 * MVP implementation — replace with Redis adapter for production multi-instance deployments.
 *
 * <p>Note: Caffeine does not support per-entry TTL natively in the standard API.
 * For MVP, we use a single cache with a global TTL configured in InfrastructureConfig.
 * Production: use Redis with per-key TTL via RedisTemplate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaffeineCacheAdapter implements CachePort {

    private final Cache<String, Object> caffeineCache;

    @Override
    public void put(String key, Object value, Duration ttl) {
        // Caffeine global TTL is set at cache construction time.
        // The ttl parameter is accepted for API compatibility but not applied per-entry.
        caffeineCache.put(key, value);
        log.debug("Cache put: key={}", key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = caffeineCache.getIfPresent(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        log.warn("Cache type mismatch for key={}: expected={}, actual={}",
                key, type.getSimpleName(), value.getClass().getSimpleName());
        return Optional.empty();
    }

    @Override
    public void evict(String key) {
        caffeineCache.invalidate(key);
        log.debug("Cache evict: key={}", key);
    }

    @Override
    public boolean exists(String key) {
        return caffeineCache.getIfPresent(key) != null;
    }
}

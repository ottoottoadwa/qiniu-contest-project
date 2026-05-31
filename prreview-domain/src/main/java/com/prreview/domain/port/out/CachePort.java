package com.prreview.domain.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Outbound port for caching arbitrary values.
 * Implemented by CaffeineCacheAdapter (MVP) or RedisCacheAdapter (production).
 */
public interface CachePort {

    /** Stores a value under the given key with a TTL. */
    void put(String key, Object value, Duration ttl);

    /** Retrieves a cached value by key. */
    <T> Optional<T> get(String key, Class<T> type);

    /** Removes a cached value. */
    void evict(String key);

    /** Checks whether a key exists in the cache. */
    boolean exists(String key);
}

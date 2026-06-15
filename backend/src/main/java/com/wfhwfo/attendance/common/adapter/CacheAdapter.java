package com.wfhwfo.attendance.common.adapter;

import java.time.Duration;
import java.util.Optional;

public interface CacheAdapter {

    Optional<String> get(String key);

    void put(String key, String value, Duration ttl);

    void evict(String key);

    void evictByPattern(String pattern);
}

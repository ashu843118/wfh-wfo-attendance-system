package com.wfhwfo.attendance.common.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CacheAdapter {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key));
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void evict(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public void evictByPattern(String pattern) {
        var keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}

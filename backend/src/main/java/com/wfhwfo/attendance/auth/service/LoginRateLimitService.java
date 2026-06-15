package com.wfhwfo.attendance.auth.service;

import com.wfhwfo.attendance.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginRateLimitService {

    private static final String KEY_PREFIX = "auth:login:attempts:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.auth.login-rate-limit.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.login-rate-limit.window-minutes:15}")
    private long windowMinutes;

    public void checkAllowed(String email) {
        String key = keyFor(email);
        String attempts = stringRedisTemplate.opsForValue().get(key);
        if (attempts != null && Integer.parseInt(attempts) >= maxAttempts) {
            throw new BusinessException(
                    "Too many login attempts. Please try again later.",
                    "AUTH_RATE_LIMITED");
        }
    }

    public void recordFailedAttempt(String email) {
        String key = keyFor(email);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }
    }

    public void resetAttempts(String email) {
        stringRedisTemplate.delete(keyFor(email));
    }

    private String keyFor(String email) {
        return KEY_PREFIX + email.trim().toLowerCase();
    }
}

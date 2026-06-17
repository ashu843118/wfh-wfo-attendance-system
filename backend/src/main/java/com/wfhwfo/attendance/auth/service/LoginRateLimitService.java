package com.wfhwfo.attendance.auth.service;

import com.wfhwfo.attendance.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitService {

    private static final String EMAIL_KEY_PREFIX = "auth:login:attempts:email:";
    private static final String IP_KEY_PREFIX = "auth:login:attempts:ip:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.auth.login-rate-limit.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.auth.login-rate-limit.window-minutes:5}")
    private long windowMinutes;

    public void checkAllowed(String email, String clientIp) {
        checkKey(emailKey(email), email, "email");
        if (StringUtils.hasText(clientIp)) {
            checkKey(ipKey(clientIp), clientIp, "ip");
        }
    }

    public void recordFailedAttempt(String email, String clientIp) {
        increment(emailKey(email));
        if (StringUtils.hasText(clientIp)) {
            increment(ipKey(clientIp));
        }
    }

    public void resetAttempts(String email) {
        stringRedisTemplate.delete(emailKey(email));
    }

    private void checkKey(String key, String identifier, String type) {
        String attempts = stringRedisTemplate.opsForValue().get(key);
        if (attempts != null && Integer.parseInt(attempts) >= maxAttempts) {
            if ("email".equals(type)) {
                log.warn("Login rate limit exceeded email={}", identifier);
            } else {
                log.warn("Login rate limit exceeded clientIp={}", identifier);
            }
            throw new BusinessException(
                    "Too many login attempts. Please try again later.",
                    "AUTH_RATE_LIMITED");
        }
    }

    private void increment(String key) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }
    }

    private String emailKey(String email) {
        return EMAIL_KEY_PREFIX + email.trim().toLowerCase();
    }

    private String ipKey(String clientIp) {
        return IP_KEY_PREFIX + clientIp.trim();
    }
}

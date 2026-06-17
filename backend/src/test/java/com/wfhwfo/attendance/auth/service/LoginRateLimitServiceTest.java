package com.wfhwfo.attendance.auth.service;

import com.wfhwfo.attendance.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginRateLimitService loginRateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginRateLimitService, "maxAttempts", 5);
        ReflectionTestUtils.setField(loginRateLimitService, "windowMinutes", 5L);
    }

    @Test
    void blocksWhenEmailAttemptsExceeded() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:login:attempts:email:employee@demo.com")).thenReturn("5");

        assertThatThrownBy(() -> loginRateLimitService.checkAllowed("employee@demo.com", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Too many login attempts. Please try again later.");
    }

    @Test
    void blocksWhenIpAttemptsExceeded() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:login:attempts:email:employee@demo.com")).thenReturn(null);
        when(valueOperations.get("auth:login:attempts:ip:127.0.0.1")).thenReturn("5");

        assertThatThrownBy(() -> loginRateLimitService.checkAllowed("employee@demo.com", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Too many login attempts. Please try again later.");
    }

    @Test
    void recordsFailedAttemptWithExpiryForEmailAndIp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:login:attempts:email:employee@demo.com")).thenReturn(1L);
        when(valueOperations.increment("auth:login:attempts:ip:127.0.0.1")).thenReturn(1L);

        loginRateLimitService.recordFailedAttempt("employee@demo.com", "127.0.0.1");

        verify(stringRedisTemplate).expire(
                eq("auth:login:attempts:email:employee@demo.com"), eq(Duration.ofMinutes(5)));
        verify(stringRedisTemplate).expire(
                eq("auth:login:attempts:ip:127.0.0.1"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void resetsEmailAttemptsOnSuccess() {
        loginRateLimitService.resetAttempts("employee@demo.com");

        verify(stringRedisTemplate).delete("auth:login:attempts:email:employee@demo.com");
    }

    @Test
    void allowsCheckWhenUnderLimit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:login:attempts:email:employee@demo.com")).thenReturn("2");
        when(valueOperations.get("auth:login:attempts:ip:127.0.0.1")).thenReturn("1");

        assertThatCode(() -> loginRateLimitService.checkAllowed("employee@demo.com", "127.0.0.1"))
                .doesNotThrowAnyException();
    }
}

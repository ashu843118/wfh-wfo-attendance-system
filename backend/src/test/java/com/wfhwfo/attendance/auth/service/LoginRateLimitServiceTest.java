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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        ReflectionTestUtils.setField(loginRateLimitService, "windowMinutes", 15L);
    }

    @Test
    void blocksWhenAttemptsExceeded() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:login:attempts:employee@demo.com")).thenReturn("5");

        assertThatThrownBy(() -> loginRateLimitService.checkAllowed("employee@demo.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    void recordsFailedAttemptWithExpiry() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("auth:login:attempts:employee@demo.com")).thenReturn(1L);

        loginRateLimitService.recordFailedAttempt("employee@demo.com");

        verify(stringRedisTemplate).expire(eq("auth:login:attempts:employee@demo.com"), eq(Duration.ofMinutes(15)));
    }

    @Test
    void resetsAttemptsOnSuccess() {
        loginRateLimitService.resetAttempts("employee@demo.com");

        verify(stringRedisTemplate).delete("auth:login:attempts:employee@demo.com");
    }
}

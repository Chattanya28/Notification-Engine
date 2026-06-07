package com.notification.gateway;

import com.notification.gateway.service.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class RateLimiterTest {

    private RateLimiter rateLimiter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimiter = new RateLimiter(redisTemplate);
    }

    @Test
    public void testRateLimiting_InMemoryFallback() {
        String keyHash = "test_key_hash";

        // Since we are mocking redisTemplate, any call to it will throw an exception
        // unless explicitly stubbed. Let's make it throw an exception to trigger the in-memory fallback!
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis unavailable"));

        // Trigger 100 requests (the limit)
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiter.isAllowed(keyHash), "Request " + (i + 1) + " should be allowed");
        }

        // The 101st request should be blocked
        assertFalse(rateLimiter.isAllowed(keyHash), "Request 101 should be blocked");
    }

    @Test
    public void testRateLimiting_RedisSuccess() {
        String keyHash = "test_key_hash_redis";
        String redisKey = "ratelimit:" + keyHash;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Mock Redis increment returning 1 (first request)
        when(valueOperations.increment(redisKey)).thenReturn(1L);
        assertTrue(rateLimiter.isAllowed(keyHash));

        // Mock Redis increment returning 100 (at limit request)
        when(valueOperations.increment(redisKey)).thenReturn(100L);
        assertTrue(rateLimiter.isAllowed(keyHash));

        // Mock Redis increment returning 101 (over limit request)
        when(valueOperations.increment(redisKey)).thenReturn(101L);
        assertFalse(rateLimiter.isAllowed(keyHash));
    }
}

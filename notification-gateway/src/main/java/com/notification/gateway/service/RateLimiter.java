package com.notification.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;
    
    // Fallback in-memory rate limiter: Map of keyHash -> timestamps of requests
    private final ConcurrentHashMap<String, Queue<Long>> localLimiters = new ConcurrentHashMap<>();
    
    private static final int LIMIT = 100;
    private static final long WINDOW_MS = 60000; // 1 minute

    /**
     * Checks if the request is allowed under the rate limit.
     */
    public boolean isAllowed(String keyHash) {
        try {
            // Attempt Redis rate limiting
            return checkRedisLimit(keyHash);
        } catch (Exception e) {
            log.warn("Redis is unavailable for rate limiting. Falling back to in-memory rate limiter. Reason: {}", e.getMessage());
            return checkInMemoryLimit(keyHash);
        }
    }

    private boolean checkRedisLimit(String keyHash) {
        String key = "ratelimit:" + keyHash;
        
        // Use Redis increment
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        
        if (count == 1) {
            // New window, set TTL
            redisTemplate.expire(key, WINDOW_MS, TimeUnit.MILLISECONDS);
        }
        
        return count <= LIMIT;
    }

    private boolean checkInMemoryLimit(String keyHash) {
        long now = System.currentTimeMillis();
        long limitTime = now - WINDOW_MS;

        // Retrieve or create the sliding window queue for this key
        Queue<Long> timestamps = localLimiters.computeIfAbsent(keyHash, k -> new ConcurrentLinkedQueue<>());

        // Synchronize on the queue for thread safety per API Key
        synchronized (timestamps) {
            // Remove timestamps older than the sliding window
            while (!timestamps.isEmpty() && timestamps.peek() < limitTime) {
                timestamps.poll();
            }

            if (timestamps.size() < LIMIT) {
                timestamps.add(now);
                return true;
            } else {
                return false;
            }
        }
    }
}

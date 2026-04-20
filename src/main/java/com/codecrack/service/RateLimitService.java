package com.codecrack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_SUBMISSIONS_PER_MINUTE = 5;
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    public boolean isSubmissionAllowed(Long userId) {
        String key = "rate:submission:" + userId;
        return isAllowed(key, MAX_SUBMISSIONS_PER_MINUTE, Duration.ofMinutes(1));
    }

    public boolean isRequestAllowed(String identifier) {
        String key = "rate:request:" + identifier;
        return isAllowed(key, MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
    }

    private boolean isAllowed(String key, int maxRequests, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, window);
        }
        if (count > maxRequests) {
            log.warn("Rate limit exceeded for key: {}", key);
            return false;
        }
        return true;
    }
}
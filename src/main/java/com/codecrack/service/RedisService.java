package com.codecrack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Service - Leaderboard, Rate Limiting, JWT Blacklist, Cache
 * Only loads when RedisTemplate is available (excluded in dev profile)
 */
@Service
@ConditionalOnBean(RedisTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LEADERBOARD_KEY = "leaderboard:global";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final int SUBMISSIONS_PER_MINUTE = 10;
    private static final int API_REQUESTS_PER_MINUTE = 100;

    // ========== LEADERBOARD ==========

    public void updateLeaderboard(String userId, int problemsSolved, int totalSubmissions) {
        try {
            double acceptanceRate = totalSubmissions > 0
                    ? (double) problemsSolved / totalSubmissions * 100
                    : 0;
            double score = problemsSolved * 100 + acceptanceRate;
            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, userId, score);
            log.info("Updated leaderboard for user {}: score={}", userId, score);
        } catch (Exception e) {
            log.error("Failed to update leaderboard for user {}: {}", userId, e.getMessage());
        }
    }

    public Set<ZSetOperations.TypedTuple<Object>> getTopUsers(int limit) {
        try {
            return redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);
        } catch (Exception e) {
            log.error("Failed to get top users: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    public Long getUserRank(String userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, userId);
        return rank != null ? rank + 1 : null;
    }

    // ========== RATE LIMITING ==========

    public boolean checkSubmissionRateLimit(String userId) {
        String key = RATE_LIMIT_PREFIX + "submission:" + userId;
        return checkRateLimit(key, SUBMISSIONS_PER_MINUTE, 60);
    }

    public boolean checkApiRateLimit(String userId) {
        String key = RATE_LIMIT_PREFIX + "api:" + userId;
        return checkRateLimit(key, API_REQUESTS_PER_MINUTE, 60);
    }

    private boolean checkRateLimit(String key, int maxRequests, int windowSeconds) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            boolean allowed = currentCount != null && currentCount <= maxRequests;
            if (!allowed) {
                log.warn("Rate limit exceeded for key: {}, count: {}", key, currentCount);
            }
            return allowed;
        } catch (Exception e) {
            log.error("Rate limit check failed for key {}: {}", key, e.getMessage());
            return true; // fail open
        }
    }

    // ========== JWT BLACKLIST ==========

    public void blacklistToken(String token, Duration expirationTime) {
        try {
            String key = JWT_BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "revoked", expirationTime);
            log.info("Token blacklisted");
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            String key = JWT_BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check token blacklist: {}", e.getMessage());
            return false;
        }
    }

    // ========== CACHE ==========

    public void invalidateUserCache(String userId) {
        try {
            Set<String> keys = redisTemplate.keys("*:user:" + userId + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} cache keys for user {}", keys.size(), userId);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache for user {}: {}", userId, e.getMessage());
        }
    }
}

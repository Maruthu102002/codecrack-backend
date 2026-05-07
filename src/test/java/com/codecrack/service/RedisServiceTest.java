package com.codecrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
        lenient().when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void updateLeaderboard_ValidUser_AddsToZSet() {
        when(zSetOperations.add(anyString(), any(), anyDouble())).thenReturn(true);
        redisService.updateLeaderboard("user1", 10, 20);
        verify(zSetOperations).add(anyString(), eq("user1"), anyDouble());
    }

    @Test
    void getUserRank_ValidUser_ReturnsRank() {
        when(zSetOperations.reverseRank(anyString(), eq("user1"))).thenReturn(0L);
        Long rank = redisService.getUserRank("user1");
        assertEquals(1L, rank);
    }

    @Test
    void getUserRank_NotFound_ReturnsNull() {
        when(zSetOperations.reverseRank(anyString(), eq("unknown"))).thenReturn(null);
        assertNull(redisService.getUserRank("unknown"));
    }

    @Test
    void checkSubmissionRateLimit_FirstRequest_ReturnsTrue() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        assertTrue(redisService.checkSubmissionRateLimit("user1"));
    }

    @Test
    void checkSubmissionRateLimit_OverLimit_ReturnsFalse() {
        when(valueOperations.increment(anyString())).thenReturn(11L);
        assertFalse(redisService.checkSubmissionRateLimit("user1"));
    }

    @Test
    void checkApiRateLimit_UnderLimit_ReturnsTrue() {
        when(valueOperations.increment(anyString())).thenReturn(50L);
        assertTrue(redisService.checkApiRateLimit("user1"));
    }

    @Test
    void isTokenBlacklisted_BlacklistedToken_ReturnsTrue() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        assertTrue(redisService.isTokenBlacklisted("some-token"));
    }

    @Test
    void isTokenBlacklisted_ValidToken_ReturnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        assertFalse(redisService.isTokenBlacklisted("valid-token"));
    }

    @Test
    void blacklistToken_CallsRedisSet() {
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        redisService.blacklistToken("token123", Duration.ofHours(1));
        verify(valueOperations).set(anyString(), eq("revoked"), any(Duration.class));
    }
}
package com.codecrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
        lenient().when(redisTemplate.expire(anyString(), any())).thenReturn(true);
    }

    @Test
    void isSubmissionAllowed_FirstRequest_ReturnsTrue() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        assertTrue(rateLimitService.isSubmissionAllowed(1L));
    }

    @Test
    void isSubmissionAllowed_UnderLimit_ReturnsTrue() {
        when(valueOperations.increment(anyString())).thenReturn(3L);
        assertTrue(rateLimitService.isSubmissionAllowed(1L));
    }

    @Test
    void isSubmissionAllowed_OverLimit_ReturnsFalse() {
        when(valueOperations.increment(anyString())).thenReturn(6L);
        assertFalse(rateLimitService.isSubmissionAllowed(1L));
    }

    @Test
    void isSubmissionAllowed_DifferentUsers_IndependentLimits() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        assertTrue(rateLimitService.isSubmissionAllowed(1L));
        assertTrue(rateLimitService.isSubmissionAllowed(2L));
    }
}
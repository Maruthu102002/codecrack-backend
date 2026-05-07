package com.codecrack.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitingFilter = new RateLimitingFilter(redisTemplate);
    }

    @Test
    void doFilter_NonAuthPath_PassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/submissions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        rateLimitingFilter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_AuthPath_UnderLimit_PassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:127.0.0.1")).thenReturn(1L);

        rateLimitingFilter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(redisTemplate).expire(eq("rate_limit:127.0.0.1"), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void doFilter_AuthPath_OverLimit_Returns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:127.0.0.1")).thenReturn(11L);

        rateLimitingFilter.doFilterInternal(request, response, chain);

        assertEquals(429, response.getStatus());
    }
}
package com.codecrack.security;

import com.codecrack.model.User;
import com.codecrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private EnhancedJwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @InjectMocks private JwtAuthFilter jwtAuthFilter;

    @Test
    void doFilter_NoAuthHeader_PassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_InvalidAuthHeader_PassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic sometoken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_ValidToken_SetsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        User mockUser = User.builder()
                .id(1L).username("maruthu").password("encoded")
                .roles(Set.of("ROLE_USER")).isActive(true).build();

        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn("maruthu");
        when(userRepository.findByUsername("maruthu")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.validateAccessToken(anyString(), any())).thenReturn(true);

        SecurityContextHolder.clearContext();
        jwtAuthFilter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_InvalidToken_DoesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtil.extractUsername("invalid.token"))
                .thenThrow(new RuntimeException("Invalid token"));

        SecurityContextHolder.clearContext();
        jwtAuthFilter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_UserNotFound_DoesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtil.extractUsername("valid.token")).thenReturn("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        SecurityContextHolder.clearContext();
        jwtAuthFilter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
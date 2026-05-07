package com.codecrack.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private EnhancedJwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new EnhancedJwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "TestSecretKeyForJWTMustBe256BitsLongEnoughForHS512Algorithm123456789");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 604800000L);
        jwtUtil.init();
    }

    private UserDetails mockUser(String username) {
        return User.withUsername(username)
                .password("pass")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void generateAccessToken_ValidUser_ReturnsToken() {
        String token = jwtUtil.generateAccessToken(mockUser("maruthu"));
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateRefreshToken_ValidUser_ReturnsToken() {
        String token = jwtUtil.generateRefreshToken(mockUser("maruthu"));
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_ValidToken_ReturnsUsername() {
        String token = jwtUtil.generateAccessToken(mockUser("maruthu"));
        String username = jwtUtil.extractUsername(token);
        assertEquals("maruthu", username);
    }

    @Test
    void validateAccessToken_ValidToken_ReturnsTrue() {
        UserDetails user = mockUser("maruthu");
        String token = jwtUtil.generateAccessToken(user);
        assertTrue(jwtUtil.validateAccessToken(token, user));
    }

    @Test
    void validateAccessToken_WrongUser_ReturnsFalse() {
        UserDetails user1 = mockUser("maruthu");
        UserDetails user2 = mockUser("other");
        String token = jwtUtil.generateAccessToken(user1);
        assertFalse(jwtUtil.validateAccessToken(token, user2));
    }

    @Test
    void validateRefreshToken_ValidToken_ReturnsTrue() {
        String token = jwtUtil.generateRefreshToken(mockUser("maruthu"));
        assertTrue(jwtUtil.validateRefreshToken(token));
    }

    @Test
    void generateTokenPair_ReturnsBothTokens() {
        EnhancedJwtUtil.TokenPair pair = jwtUtil.generateTokenPair(mockUser("maruthu"));
        assertNotNull(pair.getAccessToken());
        assertNotNull(pair.getRefreshToken());
        assertNotNull(pair.getExpiresIn());
    }
}
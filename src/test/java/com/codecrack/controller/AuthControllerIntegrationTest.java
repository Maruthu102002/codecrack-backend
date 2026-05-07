package com.codecrack.controller;

import com.codecrack.model.User;
import com.codecrack.repository.UserRepository;
import com.codecrack.security.EnhancedJwtUtil;
import com.codecrack.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EnhancedJwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private UserRepository userRepository;

    @Test
    void register_ValidRequest_Returns200() throws Exception {
        User mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .build();
        when(userService.register(anyString(), anyString(), anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"test@test.com\",\"password\":\"test123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void register_DuplicateUsername_Returns400() throws Exception {
        when(userService.register(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already taken"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dupuser\",\"email\":\"dup@test.com\",\"password\":\"test123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"notanemail\",\"password\":\"test123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        User mockUser = User.builder()
                .id(1L)
                .username("loginuser")
                .email("login@test.com")
                .password("encoded")
                .isActive(true)
                .roles(new java.util.HashSet<>(java.util.List.of("ROLE_USER")))
                .build();

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doNothing().when(valueOps).set(anyString(), anyString(), anyLong(), any());

        when(userService.findByUsername("loginuser")).thenReturn(mockUser);
        when(passwordEncoder.matches("test123", "encoded")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"loginuser\",\"password\":\"test123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void login_InvalidPassword_Returns400() throws Exception {
        User mockUser = User.builder()
                .id(1L)
                .username("wrongpass")
                .password("encoded")
                .isActive(true)
                .build();
        when(userService.findByUsername("wrongpass")).thenReturn(mockUser);
        when(passwordEncoder.matches("wrongpassword", "encoded")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wrongpass\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }
}
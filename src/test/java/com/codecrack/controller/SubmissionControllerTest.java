package com.codecrack.controller;

import com.codecrack.filter.RateLimitingFilter;
import com.codecrack.model.Submission;
import com.codecrack.model.Verdict;
import com.codecrack.security.EnhancedJwtUtil;
import com.codecrack.security.UserPrincipal;
import com.codecrack.service.RateLimitService;
import com.codecrack.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import com.codecrack.repository.UserRepository;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubmissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private EnhancedJwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    private UsernamePasswordAuthenticationToken mockAuth() {
        UserPrincipal principal = new UserPrincipal(
                1L, "testuser", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return new UsernamePasswordAuthenticationToken(
                principal, "encoded", principal.getAuthorities());
    }

    private Submission mockSubmission() {
        Submission s = new Submission();
        s.setId(1L);
        s.setUserId(1L);
        s.setProblemId(1L);
        s.setLanguage("PYTHON");
        s.setVerdict(Verdict.ACCEPTED);
        s.setSubmittedAt(java.time.LocalDateTime.now());
        s.setRuntimeMs(100);
        s.setMemoryKb(256);
        s.setErrorMessage("");
        return s;
    }

    @Test
    void getSubmission_ValidId_Returns200() throws Exception {
        when(submissionService.getSubmission(1L)).thenReturn(mockSubmission());

        mockMvc.perform(get("/api/submissions/1")
                        .with(authentication(mockAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
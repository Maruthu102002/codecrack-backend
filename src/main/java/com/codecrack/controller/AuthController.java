package com.codecrack.controller;

import com.codecrack.dto.LoginRequest;
import com.codecrack.dto.RegisterRequest;
import com.codecrack.model.User;
import com.codecrack.security.EnhancedJwtUtil;
import com.codecrack.security.UserPrincipal;
import com.codecrack.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EnhancedJwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "username", user.getUsername()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid username or password"));
        }

        if (!user.getIsActive()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Account is disabled"));
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(r -> r.replace("ROLE_", ""))
                        .toArray(String[]::new))
                .build();

        String token = jwtUtil.generateAccessToken(userDetails);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "userId", user.getId()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.findByUsername(principal.getUsername());

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "problemsSolved", user.getProblemsSolved(),
                "totalSubmissions", user.getTotalSubmissions(),
                "acceptedSubmissions", user.getAcceptedSubmissions()
        ));
    }
}
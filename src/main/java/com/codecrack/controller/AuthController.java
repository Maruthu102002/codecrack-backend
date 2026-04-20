package com.codecrack.controller;

import com.codecrack.dto.LoginRequest;
import com.codecrack.dto.RegisterRequest;
import com.codecrack.model.User;
import com.codecrack.repository.UserRepository;
import com.codecrack.security.EnhancedJwtUtil;
import com.codecrack.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnhancedJwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already taken"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already registered"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of("ROLE_USER"))
                .problemsSolved(0)
                .totalSubmissions(0)
                .acceptedSubmissions(0)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", request.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "username", request.getUsername()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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

        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

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
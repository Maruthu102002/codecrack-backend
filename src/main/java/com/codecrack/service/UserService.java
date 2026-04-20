package com.codecrack.service;

import com.codecrack.exception.ResourceNotFoundException;
import com.codecrack.model.User;
import com.codecrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(Set.of("ROLE_USER"))
                .problemsSolved(0)
                .totalSubmissions(0)
                .acceptedSubmissions(0)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", username);
        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + username));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + id));
    }
}
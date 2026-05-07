package com.codecrack.service;

import com.codecrack.model.User;
import com.codecrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_ValidUser_ReturnsUser() {
        when(userRepository.existsByUsername("maruthu")).thenReturn(false);
        when(userRepository.existsByEmail("m@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = userService.register("maruthu", "m@test.com", "pass123");

        assertNotNull(result);
        assertEquals("maruthu", result.getUsername());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername("maruthu")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                userService.register("maruthu", "m@test.com", "pass123")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByUsername("maruthu")).thenReturn(false);
        when(userRepository.existsByEmail("m@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                userService.register("maruthu", "m@test.com", "pass123")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_ValidUser_ReturnsUser() {
        User mockUser = User.builder()
                .id(1L)
                .username("maruthu")
                .email("m@test.com")
                .build();
        when(userRepository.findByUsername("maruthu")).thenReturn(Optional.of(mockUser));

        User result = userService.findByUsername("maruthu");

        assertNotNull(result);
        assertEquals("maruthu", result.getUsername());
    }

    @Test
    void findByUsername_NotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                userService.findByUsername("unknown")
        );
    }
}
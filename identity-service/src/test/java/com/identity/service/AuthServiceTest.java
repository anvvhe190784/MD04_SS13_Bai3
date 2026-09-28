package com.identity.service;

import com.identity.dto.AuthResponse;
import com.identity.dto.LoginRequest;
import com.identity.dto.RegisterRequest;
import com.identity.dto.UserResponse;
import com.identity.entity.User;
import com.identity.repository.UserRepository;
import com.identity.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("alice", "rawPassword123", "USER");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("$2a$10$hashedPasswordHere");

        UUID generatedId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(generatedId);
            return u;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("alice", response.username());
        assertEquals("USER", response.role());
        assertEquals(generatedId, response.id());
        verify(passwordEncoder).encode("rawPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest("alice", "pass", "USER");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        User user = new User("alice", "$2a$10$hashedPassword", "USER");
        LoginRequest request = new LoginRequest("alice", "password123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.token());
        verify(passwordEncoder).matches("password123", user.getPassword());
    }

    @Test
    void login_WrongPassword_ThrowsBadCredentialsException() {
        User user = new User("alice", "$2a$10$hashedPassword", "USER");
        LoginRequest request = new LoginRequest("alice", "wrongPassword");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Bad credentials", ex.getMessage());
    }

    @Test
    void login_UserNotFound_ThrowsBadCredentialsException() {
        LoginRequest request = new LoginRequest("nonexistent", "anyPassword");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Bad credentials", ex.getMessage());
    }
}

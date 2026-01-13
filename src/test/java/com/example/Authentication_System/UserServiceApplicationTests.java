package com.example.Authentication_System;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.ProfileUpdateRequest;
import com.example.Authentication_System.Domain.model.RefreshToken;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.model.Role;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RefreshTokenRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RoleRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRoleRepository;
import com.example.Authentication_System.Security.JwtUtils;
import com.example.Authentication_System.Services.AuditService;
import com.example.Authentication_System.Services.UserServiceImplementations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceApplicationTests {

    @InjectMocks
    private UserServiceImplementations userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AuditService auditService;

    @Test
    void register_Successful() {
        User inputUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("password123")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-pass");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .userType("job_seeker")
                .status("active")
                .emailVerified(false)
                .mfaEnabled(false)
                .mfaSecret(null)
                .googleId(null)
                .avatarUrl(null)
                .phone(null)
                .location(null)
                .bio(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginAt(null)
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);
        
        // Mock role repository for default role assignment
        Role jobSeekerRole = new Role(UUID.randomUUID(), "job_seeker", "Job Seeker Role", null);
        when(roleRepository.findByName("job_seeker")).thenReturn(Optional.of(jobSeekerRole));

        User registered = userService.register(inputUser, "127.0.0.1", "TestAgent");

        assertNotNull(registered);
        assertNotNull(registered.getId());
        assertEquals("hashed-pass", registered.getPasswordHash());
        assertEquals("John", registered.getFirstName());
        assertEquals("Doe", registered.getLastName());
        assertEquals("john@example.com", registered.getEmail());
        assertEquals("job_seeker", registered.getUserType());
        assertEquals("active", registered.getStatus());
        assertNotNull(registered.getCreatedAt());
        assertNotNull(registered.getUpdatedAt());

        verify(userRepository, times(1)).save(any(User.class));
        verify(auditService, times(1)).logEvent(any(), eq("REGISTER"), any(), any(), any(), any(), any());
    }

    @Test
    void refreshToken_Successful() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hashed-token")
                .expiresAt(Instant.now().plusMillis(604800000))
                .build();

        // Mock getting userId from token
        when(jwtUtils.getUserIdFromToken("refresh-token")).thenReturn(userId.toString());
        
        // Mock finding all tokens for user
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId))
                .thenReturn(List.of(refreshToken));

        // Mock matching the token hash
        when(passwordEncoder.matches("refresh-token", "hashed-token")).thenReturn(true);

        when(passwordEncoder.encode("new-refresh-token"))
                .thenReturn("new-hashed-token");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(jwtUtils.generateAccessToken(user))
                .thenReturn("new-access-token");

        when(jwtUtils.generateRefreshToken(user))
                .thenReturn("new-refresh-token");

        AuthResponse result = userService.refreshToken("refresh-token", "127.0.0.1", "TestAgent");

        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        
        verify(auditService, times(1)).logEvent(any(), eq("TOKEN_REFRESH"), any(), any(), any(), any(), any());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(new User()));

        User inputUser = User.builder()
                .email("john@example.com")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                userService.register(inputUser, "127.0.0.1", "TestAgent"));
    }


    @Test
    void login_Successful() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .status("active")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(existing));

        when(passwordEncoder.matches("password123", "hashed-pass"))
                .thenReturn(true);

        when(jwtUtils.generateAccessToken(existing))
                .thenReturn("access-token");

        when(jwtUtils.generateRefreshToken(existing))
                .thenReturn("refresh-token");

        Optional<AuthResponse> result =
                userService.login("john@example.com", "password123", "127.0.0.1", "TestAgent");

        assertTrue(result.isPresent());
        assertEquals("access-token", result.get().getAccessToken());
        assertEquals("refresh-token", result.get().getRefreshToken());
        
        verify(auditService, times(1)).logEvent(any(), eq("LOGIN_SUCCESS"), any(), any(), any(), any(), any());
    }

    @Test
    void login_WrongPassword_ReturnsEmpty() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .status("active")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(existing));

        when(passwordEncoder.matches("wrong", "hashed-pass"))
                .thenReturn(false);

        Optional<AuthResponse> result =
                userService.login("john@example.com", "wrong", "127.0.0.1", "TestAgent");

        assertTrue(result.isEmpty());
        verify(auditService, times(1)).logEvent(any(), eq("LOGIN_FAILED"), any(), any(), any(), any(), any());
    }

    @Test
    void login_UserNotFound_ReturnsEmpty() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        Optional<AuthResponse> result =
                userService.login("unknown@example.com", "pass", "127.0.0.1", "TestAgent");

        assertTrue(result.isEmpty());
        verify(auditService, times(1)).logEvent(any(), eq("LOGIN_FAILED"), any(), any(), any(), any(), any());
    }


    @Test
    void findByEmail_ReturnsUser() {
        User user = new User();
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("john@example.com");

        assertTrue(result.isPresent());
    }


    @Test
    void findById_ReturnsUser() {
        UUID id = UUID.randomUUID();
        User user = new User();

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(id);

        assertTrue(result.isPresent());
    }


    @Test
    void updateProfile_Successful() {
        UUID id = UUID.randomUUID();
        User existing = User.builder()
                .id(id)
                .firstName("Old Name")
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .userType("USER")
                .emailVerified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProfileUpdateRequest updateRequest = ProfileUpdateRequest.builder()
                .firstName("New")
                .lastName("Name")
                .build();

        when(userRepository.findById(id))
                .thenReturn(Optional.of(existing));

        userService.updateProfile(id, updateRequest, "127.0.0.1", "TestAgent");

        verify(userRepository, times(1)).save(any(User.class));
        verify(auditService, times(1)).logEvent(any(), eq("PROFILE_UPDATE"), any(), any(), any(), any(), any());
    }

    @Test
    void updateProfile_UserNotFound_ThrowsException() {
        UUID id = UUID.randomUUID();
        ProfileUpdateRequest updateRequest = ProfileUpdateRequest.builder().build();

        when(userRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateProfile(id, updateRequest, "127.0.0.1", "TestAgent"));
    }
}
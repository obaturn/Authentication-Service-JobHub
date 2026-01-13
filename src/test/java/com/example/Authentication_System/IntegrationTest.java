package com.example.Authentication_System;

import com.example.Authentication_System.Domain.model.*;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.*;
import com.example.Authentication_System.Security.JwtUtils;
import com.example.Authentication_System.Services.AuditService;
import com.example.Authentication_System.Services.UserServiceImplementations;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserServiceImplementations userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash("hashedPassword")
                .userType("job_seeker")
                .status("active")
                .emailVerified(false)
                .mfaEnabled(false)
                .mfaSecret(null)
                .googleId(null)
                .avatarUrl(null)
                .phone("1234567890")
                .location("New York")
                .bio("Test bio")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginAt(null)
                .build();
    }

    @Test
    void completeAuthenticationFlow_ShouldWorkEndToEnd() {
        // Step 1: Register
        User newUser = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .passwordHash("password123")
                .phone("0987654321")
                .location("LA")
                .bio("New bio")
                .build();

        Role jobSeekerRole = Role.builder().id(UUID.randomUUID()).name("job_seeker").build();

        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(roleRepository.findByName("job_seeker")).thenReturn(Optional.of(jobSeekerRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(mock(UserRole.class));

        User registeredUser = userService.register(newUser, "127.0.0.1", "Mozilla/5.0");
        assertNotNull(registeredUser);
        assertEquals("john.doe@example.com", registeredUser.getEmail()); // Assuming save returns testUser

        // Step 2: Login
        String loginEmail = "john.doe@example.com";
        String loginPassword = "password123";

        when(userRepository.findByEmail(loginEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginPassword, testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("refreshToken");
        when(passwordEncoder.encode("refreshToken")).thenReturn("hashedRefreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<AuthResponse> loginResponse = userService.login(loginEmail, loginPassword, "127.0.0.1", "Mozilla/5.0");
        assertTrue(loginResponse.isPresent());
        AuthResponse authResponse = loginResponse.get();
        assertEquals("accessToken", authResponse.getAccessToken());
        assertEquals("refreshToken", authResponse.getRefreshToken());

        // Step 3: Refresh Token
        String refreshTokenValue = "refreshToken";
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hashedRefreshToken")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(null)
                .build();

        when(jwtUtils.getUserIdFromToken(refreshTokenValue)).thenReturn(userId.toString());
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(refreshToken));
        when(passwordEncoder.matches(refreshTokenValue, "hashedRefreshToken")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("newAccessToken");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("newRefreshToken");
        when(passwordEncoder.encode("newRefreshToken")).thenReturn("newHashedRefreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));

        AuthResponse refreshResponse = userService.refreshToken(refreshTokenValue, "127.0.0.1", "Mozilla/5.0");
        assertNotNull(refreshResponse);
        assertEquals("newAccessToken", refreshResponse.getAccessToken());
        assertEquals("newRefreshToken", refreshResponse.getRefreshToken());

        // Step 4: Logout
        userService.logout(userId, "127.0.0.1", "Mozilla/5.0");

        // Verify all interactions
        verify(userRepository, times(2)).findByEmail(anyString()); // Register and login
        verify(userRepository, times(2)).save(any(User.class)); // Register and login update
        verify(roleRepository).findByName("job_seeker");
        verify(userRoleRepository).save(any(UserRole.class));
        verify(passwordEncoder, times(2)).matches(anyString(), anyString()); // Login and refresh (matches is called in refresh loop)
        verify(jwtUtils, times(2)).generateAccessToken(testUser); // Login and refresh
        verify(jwtUtils, times(2)).generateRefreshToken(testUser); // Login and refresh
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // Login and refresh
        verify(refreshTokenRepository).revokeToken(anyString()); // Refresh revokes old
        verify(refreshTokenRepository).revokeAllTokensForUser(userId); // Logout
        verify(auditService, times(4)).logEvent(any(), anyString(), anyString(), any(), anyString(), anyString(), anyString()); // Register, login, refresh, logout
    }

    @Test
    void roleAssignmentDuringRegistration_ShouldAssignJobSeekerRole() {
        // Arrange
        User newUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash("password")
                .build();

        Role jobSeekerRole = Role.builder().id(UUID.randomUUID()).name("job_seeker").build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(roleRepository.findByName("job_seeker")).thenReturn(Optional.of(jobSeekerRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(mock(UserRole.class));

        // Act
        User result = userService.register(newUser, "127.0.0.1", "Mozilla/5.0");

        // Assert
        verify(roleRepository).findByName("job_seeker");
        verify(userRoleRepository).save(argThat(userRole ->
                userRole.getUserId().equals(result.getId()) &&
                userRole.getRoleId().equals(jobSeekerRole.getId())
        ));
    }

    @Test
    void auditLoggingAcrossOperations_ShouldLogAllEvents() {
        // Arrange
        User newUser = User.builder()
                .firstName("Audit")
                .lastName("Test")
                .email("audit@example.com")
                .passwordHash("password")
                .build();

        Role jobSeekerRole = Role.builder().id(UUID.randomUUID()).name("job_seeker").build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(roleRepository.findByName("job_seeker")).thenReturn(Optional.of(jobSeekerRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(mock(UserRole.class));

        // Act: Register
        userService.register(newUser, "127.0.0.1", "Mozilla/5.0");

        // Assert: Audit log for register
        verify(auditService).logEvent(eq(testUser.getId()), eq("REGISTER"), eq("User"), eq(testUser.getId()), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));

        // Act: Login
        when(userRepository.findByEmail("audit@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("token");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("refresh");
        when(passwordEncoder.encode("refresh")).thenReturn("hashedRefresh");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.login("audit@example.com", "password", "127.0.0.1", "Mozilla/5.0");

        // Assert: Audit log for login
        verify(auditService).logEvent(eq(testUser.getId()), eq("LOGIN_SUCCESS"), eq("User"), eq(testUser.getId()), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));

        // Act: Logout
        userService.logout(testUser.getId(), "127.0.0.1", "Mozilla/5.0");

        // Assert: Audit log for logout
        verify(auditService).logEvent(eq(testUser.getId()), eq("LOGOUT"), eq("User"), eq(testUser.getId()), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }
}
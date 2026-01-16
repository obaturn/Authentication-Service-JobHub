package com.example.Authentication_System;

import com.example.Authentication_System.Domain.model.*;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.*;
import com.example.Authentication_System.Security.JwtUtils;
import com.example.Authentication_System.Services.AuditService;
import com.example.Authentication_System.Services.EmailService;
import com.example.Authentication_System.Services.MfaService;
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
class UserServiceImplementationsTest {

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

    @Mock
    private EmailService emailService;

    @Mock
    private MfaService mfaService;

    @InjectMocks
    private UserServiceImplementations userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder()
                .phone("1234567890")
                .location("New York")
                .bio("Test bio")
                .build();

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
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginAt(null)
                .build();
        testUser.setUserProfile(userProfile);
    }

    @Test
    void register_WithValidData_ShouldRegisterUser() {
        // Arrange
        UserProfile profile = UserProfile.builder().phone("0987654321").location("LA").bio("New bio").build();
        User newUser = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .passwordHash("password123")
                .build();
        newUser.setUserProfile(profile);

        Role jobSeekerRole = Role.builder().id(UUID.randomUUID()).name("job_seeker").build();

        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(roleRepository.findByName("job_seeker")).thenReturn(Optional.of(jobSeekerRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(mock(UserRole.class));

        // Act
        User result = userService.register(newUser, "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertNotNull(result);
        verify(userRepository).findByEmail(newUser.getEmail());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(roleRepository).findByName("job_seeker");
        verify(userRoleRepository).save(any(UserRole.class));
        verify(auditService).logEvent(eq(result.getId()), eq("REGISTER"), eq("User"), eq(result.getId()), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
        verify(emailService).sendVerificationEmail(eq(result.getEmail()), anyString());
    }

    @Test
    void login_WithUnverifiedEmail_ShouldThrowException() {
        // Arrange
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            userService.login(testUser.getEmail(), "password123", "127.0.0.1", "Mozilla/5.0");
        });
    }

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyEmail() {
        // Arrange
        String token = "valid-token";
        testUser.setEmailVerified(false);
        testUser.setEmailVerificationToken(token);
        testUser.setEmailVerificationExpiresAt(Instant.now().plusSeconds(3600));
        when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userService.verifyEmail(token);

        // Assert
        assertTrue(result);
        assertTrue(testUser.isEmailVerified());
        assertEquals("active", testUser.getStatus());
        assertNull(testUser.getEmailVerificationToken());
        verify(userRepository).save(testUser);
    }

    @Test
    void forgotPassword_ShouldSendResetEmail() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        userService.forgotPassword(testUser.getEmail());

        // Assert
        assertNotNull(testUser.getPasswordResetToken());
        verify(userRepository).save(testUser);
        verify(emailService).sendPasswordResetEmail(eq(testUser.getEmail()), anyString());
    }

    @Test
    void resetPassword_WithValidToken_ShouldResetPassword() {
        // Arrange
        String token = "valid-reset-token";
        String newPassword = "newPassword123!";
        testUser.setPasswordResetToken(token);
        testUser.setPasswordResetExpiresAt(Instant.now().plusSeconds(3600));
        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("newHashedPassword");

        // Act
        userService.resetPassword(token, newPassword);

        // Assert
        assertEquals("newHashedPassword", testUser.getPasswordHash());
        assertNull(testUser.getPasswordResetToken());
        verify(userRepository).save(testUser);
    }

    @Test
    void setupMfa_ShouldReturnSecretAndQrCode() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaService.generateNewSecret()).thenReturn("test-secret");
        when(mfaService.generateQrCodeImageUri("test-secret", testUser.getEmail(), "JobHub")).thenReturn("test-qr-code");

        // Act
        MfaSetupResponse response = userService.setupMfa(userId);

        // Assert
        assertEquals("test-secret", response.getSecret());
        assertEquals("test-qr-code", response.getQrCode());
        assertEquals("test-secret", testUser.getMfaSecret());
        verify(userRepository).save(testUser);
    }

    @Test
    void enableMfa_WithValidCode_ShouldEnableMfa() {
        // Arrange
        testUser.setMfaSecret("test-secret");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaService.isOtpValid("test-secret", "123456")).thenReturn(true);

        // Act
        userService.enableMfa(userId, "123456");

        // Assert
        assertTrue(testUser.isMfaEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void login_WithMfaEnabled_ShouldReturnMfaToken() {
        // Arrange
        testUser.setEmailVerified(true);
        testUser.setMfaEnabled(true);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateToken(testUser, 300000)).thenReturn("mfa-token");

        // Act
        Optional<AuthResponse> response = userService.login(testUser.getEmail(), "password123", "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertTrue(response.isPresent());
        assertEquals("mfa-token", response.get().getMfaToken());
        assertNull(response.get().getAccessToken());
    }

    @Test
    void verifyMfa_WithValidCode_ShouldReturnAuthResponse() {
        // Arrange
        testUser.setMfaSecret("test-secret");
        String mfaToken = "mfa-token";
        when(jwtUtils.getUserIdFromToken(mfaToken)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(mfaService.isOtpValid("test-secret", "123456")).thenReturn(true);
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("refresh-token");

        // Act
        Optional<AuthResponse> response = userService.verifyMfa(mfaToken, "123456");

        // Assert
        assertTrue(response.isPresent());
        assertEquals("access-token", response.get().getAccessToken());
        verify(userRepository).save(testUser);
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Arrange
        User newUser = User.builder()
                .email("existing@example.com")
                .passwordHash("password123")
                .build();

        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(newUser, "127.0.0.1", "Mozilla/5.0"));
        assertEquals("Email already in use", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Arrange
        testUser.setEmailVerified(true);
        String email = "john.doe@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("refreshToken");
        when(passwordEncoder.encode("refreshToken")).thenReturn("hashedRefreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mock(RefreshToken.class));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<AuthResponse> result = userService.login(email, password, "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertTrue(result.isPresent());
        AuthResponse authResponse = result.get();
        assertEquals("accessToken", authResponse.getAccessToken());
        assertEquals("refreshToken", authResponse.getRefreshToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(900000L, authResponse.getExpiresIn());
        assertNotNull(authResponse.getUser());
        assertEquals(testUser.getId(), authResponse.getUser().getId());
        assertNotNull(authResponse.getUser().getLastLoginAt());
        verify(auditService).logEvent(eq(userId), eq("LOGIN_SUCCESS"), eq("User"), eq(userId), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void login_WithInvalidEmail_ShouldReturnEmpty() {
        // Arrange
        String email = "invalid@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Optional<AuthResponse> result = userService.login(email, password, "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertFalse(result.isPresent());
        verify(auditService).logEvent(isNull(), eq("LOGIN_FAILED"), eq("User"), isNull(), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void login_WithSuspendedAccount_ShouldReturnEmpty() {
        // Arrange
        testUser.setEmailVerified(true);
        User suspendedUser = testUser.toBuilder().status("suspended").build();
        String email = "john.doe@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(suspendedUser));

        // Act
        Optional<AuthResponse> result = userService.login(email, password, "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertFalse(result.isPresent());
        verify(auditService).logEvent(eq(userId), eq("LOGIN_FAILED"), eq("User"), eq(userId), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void login_WithInvalidPassword_ShouldReturnEmpty() {
        // Arrange
        testUser.setEmailVerified(true);
        String email = "john.doe@example.com";
        String password = "wrongpassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPasswordHash())).thenReturn(false);

        // Act
        Optional<AuthResponse> result = userService.login(email, password, "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertFalse(result.isPresent());
        verify(auditService).logEvent(eq(userId), eq("LOGIN_FAILED"), eq("User"), eq(userId), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewAuthResponse() {
        // Arrange
        String refreshTokenValue = "validRefreshToken";
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

        // Act
        AuthResponse result = userService.refreshToken(refreshTokenValue, "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        verify(refreshTokenRepository).revokeToken(refreshToken.getTokenHash());
        verify(auditService).logEvent(eq(userId), eq("TOKEN_REFRESH"), eq("RefreshToken"), any(UUID.class), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldThrowException() {
        // Arrange
        String refreshTokenValue = "invalidRefreshToken";
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hashedRefreshToken")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(null)
                .build();

        when(jwtUtils.getUserIdFromToken(refreshTokenValue)).thenReturn(userId.toString());
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(refreshToken));
        when(passwordEncoder.matches(refreshTokenValue, "hashedRefreshToken")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.refreshToken(refreshTokenValue, "127.0.0.1", "Mozilla/5.0"));
        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refreshToken_WithExpiredToken_ShouldThrowException() {
        // Arrange
        String refreshTokenValue = "expiredRefreshToken";
        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hashedRefreshToken")
                .expiresAt(Instant.now().minusSeconds(3600))
                .revokedAt(null)
                .build();

        when(jwtUtils.getUserIdFromToken(refreshTokenValue)).thenReturn(userId.toString());
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(expiredToken));
        when(passwordEncoder.matches(refreshTokenValue, "hashedRefreshToken")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.refreshToken(refreshTokenValue, "127.0.0.1", "Mozilla/5.0"));
        assertEquals("Refresh token expired", exception.getMessage());
    }

    @Test
    void updateProfile_WithValidData_ShouldUpdateProfile() {
        // Arrange
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("UpdatedFirstName")
                .lastName("UpdatedLastName")
                .avatarUrl("newAvatar.jpg")
                .phone("1112223333")
                .location("UpdatedLocation")
                .bio("Updated bio")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.updateProfile(userId, request, "127.0.0.1", "Mozilla/5.0");

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(auditService).logEvent(eq(userId), eq("PROFILE_UPDATE"), eq("User"), eq(userId), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void updateProfile_WithInvalidUserId_ShouldThrowException() {
        // Arrange
        UUID invalidUserId = UUID.randomUUID();
        ProfileUpdateRequest request = ProfileUpdateRequest.builder().firstName("Test").build();

        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateProfile(invalidUserId, request, "127.0.0.1", "Mozilla/5.0"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void logout_ShouldRevokeTokensAndLog() {
        // Arrange
        // Act
        userService.logout(userId, "127.0.0.1", "Mozilla/5.0");

        // Assert
        verify(refreshTokenRepository).revokeAllTokensForUser(userId);
        verify(auditService).logEvent(eq(userId), eq("LOGOUT"), eq("User"), eq(userId), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void getProfile_WithValidUserId_ShouldReturnUserProfile() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<UserProfile> result = userService.getProfile(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser.getUserProfile(), result.get());
    }

    @Test
    void getProfile_WithInvalidUserId_ShouldReturnEmpty() {
        // Arrange
        UUID invalidUserId = UUID.randomUUID();
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        // Act
        Optional<UserProfile> result = userService.getProfile(invalidUserId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void deactivateAccount_WithValidUserId_ShouldDeactivate() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.deactivateAccount(userId, "127.0.0.1", "Mozilla/5.0");

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).revokeAllTokensForUser(userId);
        verify(auditService).logEvent(eq(userId), eq("ACCOUNT_DEACTIVATION"), eq("User"), eq(userId), anyString(), eq("127.0.0.1"), eq("Mozilla/5.0"));
    }

    @Test
    void deactivateAccount_WithInvalidUserId_ShouldThrowException() {
        // Arrange
        UUID invalidUserId = UUID.randomUUID();
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.deactivateAccount(invalidUserId, "127.0.0.1", "Mozilla/5.0"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // Arrange
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }

    @Test
    void findById_WithExistingId_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }
}
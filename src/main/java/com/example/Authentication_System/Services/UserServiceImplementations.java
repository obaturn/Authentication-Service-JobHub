package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.*;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RefreshTokenRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RoleRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRoleRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.OutboxEventRepository;
import com.example.Authentication_System.Infrastructure.Adapter.KafkaEventPublisher;
import com.example.Authentication_System.Security.CryptoUtils;
import com.example.Authentication_System.Security.JwtUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Service
public class UserServiceImplementations implements UserUseCase {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImplementations.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;
    private final EmailService emailService;
    private final MfaService mfaService;
    private final AccountLockoutService accountLockoutService;
    private final TokenBlacklistService tokenBlacklistService;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CryptoUtils cryptoUtils;

    public UserServiceImplementations(UserRepository userRepository,
                                         RefreshTokenRepository refreshTokenRepository,
                                         RoleRepository roleRepository,
                                         UserRoleRepository userRoleRepository,
                                         PasswordEncoder passwordEncoder,
                                         JwtUtils jwtUtils,
                                         AuditService auditService,
                                         EmailService emailService,
                                         MfaService mfaService,
                                         AccountLockoutService accountLockoutService,
                                         TokenBlacklistService tokenBlacklistService,
                                         KafkaEventPublisher kafkaEventPublisher,
                                         OutboxEventRepository outboxEventRepository,
                                         ObjectMapper objectMapper,
                                         CryptoUtils cryptoUtils) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.auditService = auditService;
        this.emailService = emailService;
        this.mfaService = mfaService;
        this.accountLockoutService = accountLockoutService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.cryptoUtils = cryptoUtils;
    }

    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public User register(User user, String ipAddress, String userAgent) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Use local encoder to ensure consistency
        BCryptPasswordEncoder localEncoder = new BCryptPasswordEncoder();
        String hashedPassword = localEncoder.encode(user.getPasswordHash());

        User newUser = User.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .passwordHash(hashedPassword) // Store the hash
                .userType(user.getUserType() != null ? user.getUserType() : "job_seeker")
                .status("pending_verification")
                .build();

        UserProfile userProfile = UserProfile.builder()
                .bio(user.getUserProfile() != null ? user.getUserProfile().getBio() : null)
                .location(user.getUserProfile() != null ? user.getUserProfile().getLocation() : null)
                .phone(user.getUserProfile() != null ? user.getUserProfile().getPhone() : null)
                .build();

        newUser.setUserProfile(userProfile);

        // Generate and set email verification token
        String token = UUID.randomUUID().toString();
        newUser.setEmailVerificationToken(token);
        newUser.setEmailVerificationExpiresAt(Instant.now().plusSeconds(172800)); // 48 hours

        User savedUser = userRepository.save(newUser);

        // Assign role based on userType
        String userType = user.getUserType() != null ? user.getUserType() : "job_seeker";
        roleRepository.findByName(userType).ifPresent(role -> {
            UserRole userRole = UserRole.builder()
                    .userId(savedUser.getId())
                    .roleId(role.getId())
                    .assignedAt(Instant.now())
                    .build();
            userRoleRepository.save(userRole);
        });

        // Save event to outbox for reliable publishing
        UserRegisteredEvent event = new UserRegisteredEvent(
                "UserRegistered",
                savedUser.getId().toString(),
                savedUser.getEmail(),
                token,
                savedUser.getFirstName(),
                Instant.now()
        );

        saveOutboxEvent(event);

        auditService.logEvent(savedUser.getId(), "REGISTER", "User", savedUser.getId(), "User registered successfully", ipAddress, userAgent);

        return savedUser;
    }


    @Override
    public Optional<AuthResponse> login(String email, String password, String ipAddress, String userAgent) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Audit log for login failure - user not found
            auditService.logEvent(null, "LOGIN_FAILED", "User", null, "Login failed: user not found for email " + email, ipAddress, userAgent);
            return Optional.empty();
        }

        User user = userOpt.get();

        // Check if account is locked before proceeding
        if (accountLockoutService.isAccountLocked(user.getId())) {
            long remainingTime = accountLockoutService.getRemainingLockoutTime(user.getId());
            auditService.logEvent(user.getId(), "LOGIN_FAILED", "User", user.getId(),
                    "Login failed: account locked for " + remainingTime + " seconds", ipAddress, userAgent);
            throw new IllegalStateException("Account is temporarily locked due to too many failed login attempts. Try again in " + remainingTime + " seconds.");
        }

        if (!user.isEmailVerified()) {
            auditService.logEvent(user.getId(), "LOGIN_FAILED", "User", user.getId(), "Login failed: email not verified", ipAddress, userAgent);
            accountLockoutService.recordFailedAttempt(user.getId(), ipAddress, userAgent);
            throw new IllegalStateException("Email not verified");
        }

        // Check user status - only allow active users to login
        if (!"active".equals(user.getStatus())) {
            // Audit log for login failure - account not active
            auditService.logEvent(user.getId(), "LOGIN_FAILED", "User", user.getId(), "Login failed: account status is " + user.getStatus(), ipAddress, userAgent);
            accountLockoutService.recordFailedAttempt(user.getId(), ipAddress, userAgent);
            return Optional.empty();
        }

        boolean matches = false;
        try {
            // Use local encoder to match registration logic
            BCryptPasswordEncoder localEncoder = new BCryptPasswordEncoder();
            matches = localEncoder.matches(password, user.getPasswordHash());
        } catch (IllegalArgumentException e) {
            logger.warn("LOGIN WARNING: BCrypt check failed with exception for user {}. Treating as invalid password. Error: {}", email, e.getMessage());
            matches = false;
        }

        if (!matches) {
            // Record failed attempt and apply progressive delay
            accountLockoutService.recordFailedAttempt(user.getId(), ipAddress, userAgent);
            long delay = accountLockoutService.getProgressiveDelay(user.getFailedLoginAttempts() + 1);

            // Audit log for login failure - invalid password
            auditService.logEvent(user.getId(), "LOGIN_FAILED", "User", user.getId(), "Login failed: invalid password", ipAddress, userAgent);

            // Apply progressive delay for rapid attempts
            if (delay > 0) {
                try {
                    Thread.sleep(delay * 1000); // Convert to milliseconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return Optional.empty();
        }

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshTokenValue = jwtUtils.generateRefreshToken(user);

        // Store refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                // .id(UUID.randomUUID()) // Removed manual ID generation to let Hibernate handle it
                .userId(user.getId())
                .tokenHash(hashRefreshToken(refreshTokenValue)) // Use SHA-256 instead of BCrypt for long tokens
                .expiresAt(Instant.now().plusMillis(604800000)) // 7 days
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        // Update last login
        User updatedUser = user.toBuilder()
                .lastLoginAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(updatedUser);

        // Record successful login (resets failed attempt counter)
        accountLockoutService.recordSuccessfulLogin(user.getId());

        // Audit log for successful login
        auditService.logEvent(user.getId(), "LOGIN_SUCCESS", "User", user.getId(), "User logged in successfully", ipAddress, userAgent);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(900000L) // 15 minutes
                .user(updatedUser)
                .build();

        return Optional.of(authResponse);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional
    public void updateProfile(UUID userId, ProfileUpdateRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setFirstName(request.getFirstName() != null ? request.getFirstName() : user.getFirstName());
        user.setLastName(request.getLastName() != null ? request.getLastName() : user.getLastName());

        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = UserProfile.builder().build();
            user.setUserProfile(userProfile);
        }
        
        userProfile.setAvatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : userProfile.getAvatarUrl());
        userProfile.setPhone(request.getPhone() != null ? request.getPhone() : userProfile.getPhone());
        userProfile.setLocation(request.getLocation() != null ? request.getLocation() : userProfile.getLocation());
        userProfile.setBio(request.getBio() != null ? request.getBio() : userProfile.getBio());

        userRepository.save(user);

        auditService.logEvent(user.getId(), "PROFILE_UPDATE", "User", user.getId(), "User profile updated", ipAddress, userAgent);
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenValue, String ipAddress, String userAgent) {
        // 1. Get all active refresh tokens for the user (we need to find the user first)
        // Since we only have the raw token, we can't query by hash directly because of the salt.
        // We need to decode the token to get the userId first.
        
        String userIdStr = jwtUtils.getUserIdFromToken(refreshTokenValue);
        if (userIdStr == null) {
             throw new IllegalArgumentException("Invalid refresh token structure");
        }
        
        UUID userId = UUID.fromString(userIdStr);
        
        // 2. Fetch all valid tokens for this user from DB
        List<RefreshToken> userTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        
        RefreshToken matchedToken = null;
        
        // 3. Iterate and check matches
        String hashedInputToken = hashRefreshToken(refreshTokenValue);
        for (RefreshToken token : userTokens) {
            // Use simple string comparison for SHA-256 hashes
            if (hashedInputToken.equals(token.getTokenHash())) {
                matchedToken = token;
                break;
            }
        }

        if (matchedToken == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Check if token is expired
        if (matchedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        // Get user
        Optional<User> userOpt = userRepository.findById(matchedToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();

        // Revoke old refresh token
        refreshTokenRepository.revokeToken(matchedToken.getTokenHash());

        // Generate new tokens
        String newAccessToken = jwtUtils.generateAccessToken(user);
        String newRefreshTokenValue = jwtUtils.generateRefreshToken(user);

        // Store new refresh token
        RefreshToken newRefreshToken = RefreshToken.builder()
                // .id(UUID.randomUUID()) // Removed manual ID generation
                .userId(user.getId())
                .tokenHash(hashRefreshToken(newRefreshTokenValue)) // Use SHA-256
                .expiresAt(Instant.now().plusMillis(604800000)) // 7 days
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(newRefreshToken);

        // Audit log for token refresh
        auditService.logEvent(user.getId(), "TOKEN_REFRESH", "RefreshToken", newRefreshToken.getId(), "Refresh token generated successfully", ipAddress, userAgent);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(900000L) // 15 minutes
                .user(user)
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID userId, String accessToken, String ipAddress, String userAgent) {
        // Blacklist the current access token
        if (accessToken != null && !accessToken.isEmpty()) {
            // Calculate token expiry (15 minutes from now as fallback)
            long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000);
            tokenBlacklistService.blacklistToken(accessToken, expiryTime);
        }

        // Revoke all refresh tokens for the user
        refreshTokenRepository.revokeAllTokensForUser(userId);

        // Audit log for logout
        auditService.logEvent(userId, "LOGOUT", "User", userId, "User logged out successfully", ipAddress, userAgent);
    }

    @Override
    public Optional<UserProfile> getProfile(UUID userId) {
        return userRepository.findById(userId).map(User::getUserProfile);
    }

    @Override
    public void deactivateAccount(UUID userId, String ipAddress, String userAgent) {
        Optional<User> existingOpt = userRepository.findById(userId);

        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User existing = existingOpt.get();

        User updated = existing.toBuilder()
                .status("inactive")
                .updatedAt(Instant.now())
                .build();

        userRepository.save(updated);

        // Revoke all refresh tokens for the user
        refreshTokenRepository.revokeAllTokensForUser(userId);

        // Audit log for account deactivation
        auditService.logEvent(userId, "ACCOUNT_DEACTIVATION", "User", userId, "User account deactivated", ipAddress, userAgent);
    }

    @Override
    @Transactional
    public void sendVerificationEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Return silently to prevent enumeration
            return;
        }

        User user = userOpt.get();
        if (user.isEmailVerified()) {
            // Return silently
            return;
        }

        String token;
        boolean isNewToken = false;
        if (user.getEmailVerificationToken() != null &&
            user.getEmailVerificationExpiresAt() != null &&
            user.getEmailVerificationExpiresAt().isAfter(Instant.now())) {
            // Use existing valid token
            token = user.getEmailVerificationToken();
            logger.info("SEND_VERIFICATION: Reusing existing valid token for user {}: {}", user.getEmail(), token);
        } else {
            // Generate new token
            token = UUID.randomUUID().toString();
            user.setEmailVerificationToken(token);
            user.setEmailVerificationExpiresAt(Instant.now().plusSeconds(172800)); // 48 hours
            isNewToken = true;
            logger.info("SEND_VERIFICATION: Generated new token for user {}: {}", user.getEmail(), token);
        }

        if (isNewToken) {
            userRepository.save(user);
        }

        // Publish event to Kafka instead of using local email service
        UserRegisteredEvent event = new UserRegisteredEvent(
                "VerificationEmailRequested",
                user.getId().toString(),
                user.getEmail(),
                token,
                user.getFirstName(),
                Instant.now()
        );

        saveOutboxEvent(event);
    }

    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        logger.info("VERIFY: Attempting to verify email with token: {}", token);

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> {
                    logger.warn("VERIFY: Verification failed: Token not found in database: {}", token);
                    return new IllegalArgumentException("Invalid verification token");
                });

        logger.info("VERIFY: Found user {} for token {}", user.getEmail(), token);

        // Check if already verified
        if (user.isEmailVerified()) {
            logger.info("VERIFY: User {} is already verified", user.getEmail());
            return true;
        }

        if (user.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
            logger.warn("VERIFY: Verification failed: Token expired for user: {}", user.getEmail());
            throw new IllegalStateException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setStatus("active");
        // Keep the token for idempotency, don't set to null
        userRepository.save(user);

        logger.info("VERIFY: Email verified successfully for user: {}", user.getEmail());

        return true;
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Return silently to prevent enumeration
            return;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(cryptoUtils.hashWithSHA256(token));
        user.setPasswordResetExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour

        userRepository.save(user);

        // Publish event to Kafka
        // Note: You'll need a PasswordResetRequestedEvent class for this
        // For now, I'll leave the email service call but you should replace it
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hashedToken = cryptoUtils.hashWithSHA256(token);
        User user = userRepository.findByPasswordResetToken(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Password reset token has expired");
        }

        logger.info("RESET: New password length before encoding: {}", newPassword.length());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String secret = mfaService.generateNewSecret();
        String qrCode = mfaService.generateQrCodeImageUri(secret, user.getEmail(), "JobHub");

        user.setMfaSecret(cryptoUtils.encrypt(secret));
        userRepository.save(user);

        return new MfaSetupResponse(secret, qrCode);
    }

    @Override
    @Transactional
    public void enableMfa(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!mfaService.isOtpValid(user.getMfaSecret(), code)) {
            throw new IllegalArgumentException("Invalid MFA code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    @Override
    public Optional<AuthResponse> verifyMfa(String mfaToken, String code) {
        String userIdStr = jwtUtils.getUserIdFromToken(mfaToken);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Invalid MFA token");
        }

        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String decryptedSecret = cryptoUtils.decrypt(user.getMfaSecret());
        if (!mfaService.isOtpValid(decryptedSecret, code)) {
            throw new IllegalArgumentException("Invalid MFA code");
        }

        // MFA code is valid, proceed with generating full auth response
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshTokenValue = jwtUtils.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(refreshTokenValue))
                .expiresAt(Instant.now().plusMillis(604800000)) // 7 days
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return Optional.of(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(900000L)
                .user(user)
                .build());
    }
    
    private void saveOutboxEvent(Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(event.getClass().getSimpleName())
                    .payload(payload)
                    .topic("user-events")
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize event to outbox: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateAvatar(UUID userId, AvatarUploadRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setAvatarUrl(request.getAvatarUrl());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        auditService.logEvent(userId, "AVATAR_UPDATE", "User", userId, "Avatar updated", ipAddress, userAgent);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllTokensForUser(userId);
        
        auditService.logEvent(userId, "PASSWORD_CHANGE", "User", userId, "Password changed successfully", ipAddress, userAgent);
    }
}
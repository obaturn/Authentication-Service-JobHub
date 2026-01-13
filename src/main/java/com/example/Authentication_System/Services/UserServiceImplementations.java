package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.ProfileUpdateRequest;
import com.example.Authentication_System.Domain.model.RefreshToken;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.model.Role;
import com.example.Authentication_System.Domain.model.UserRole;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RefreshTokenRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RoleRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRoleRepository;
import com.example.Authentication_System.Security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Service
public class UserServiceImplementations implements UserUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;

    public UserServiceImplementations(UserRepository userRepository,
                                      RefreshTokenRepository refreshTokenRepository,
                                      RoleRepository roleRepository,
                                      UserRoleRepository userRoleRepository,
                                      PasswordEncoder passwordEncoder,
                                      JwtUtils jwtUtils,
                                      AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.auditService = auditService;
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
    public User register(User user, String ipAddress, String userAgent) {
        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }


        String hashedPassword = passwordEncoder.encode(user.getPasswordHash());


        Instant now = Instant.now();

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .passwordHash(hashedPassword)
                .userType(user.getUserType() != null ? user.getUserType() : "job_seeker")
                .status(user.getStatus() != null ? user.getStatus() : "active")
                .emailVerified(false)
                .mfaEnabled(false)
                .mfaSecret(null)
                .googleId(null)
                .avatarUrl(null)
                .phone(user.getPhone())
                .location(user.getLocation())
                .bio(user.getBio())
                .createdAt(now)
                .updatedAt(now)
                .lastLoginAt(null)
                .build();

        User savedUser = userRepository.save(newUser);

        // Assign default "job_seeker" role to new user
        Optional<Role> jobSeekerRoleOpt = roleRepository.findByName("job_seeker");
        if (jobSeekerRoleOpt.isPresent()) {
            Role jobSeekerRole = jobSeekerRoleOpt.get();
            UserRole userRole = UserRole.builder()
                    .id(UUID.randomUUID())
                    .userId(savedUser.getId())
                    .roleId(jobSeekerRole.getId())
                    .assignedAt(Instant.now())
                    .assignedBy(savedUser.getId()) // Self-assigned during registration
                    .build();
            userRoleRepository.save(userRole);
        }

        // Audit log for user registration
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

        // Check user status - only allow active users to login
        if (!"active".equals(user.getStatus())) {
            // Audit log for login failure - account not active
            auditService.logEvent(user.getId(), "LOGIN_FAILED", "User", user.getId(), "Login failed: account status is " + user.getStatus(), ipAddress, userAgent);
            return Optional.empty();
        }

        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        if (!matches) {
            // Audit log for login failure - invalid password
            auditService.logEvent(user.getId(), "LOGIN_FAILED", "User", user.getId(), "Login failed: invalid password", ipAddress, userAgent);
            return Optional.empty();
        }

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshTokenValue = jwtUtils.generateRefreshToken(user);

        // Store refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(refreshTokenValue))
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
    public void updateProfile(UUID userId, ProfileUpdateRequest request, String ipAddress, String userAgent) {
        Optional<User> existingOpt = userRepository.findById(userId);

        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User existing = existingOpt.get();

        User saved = existing.toBuilder()
                .firstName(request.getFirstName() != null ? request.getFirstName() : existing.getFirstName())
                .lastName(request.getLastName() != null ? request.getLastName() : existing.getLastName())
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : existing.getAvatarUrl())
                .phone(request.getPhone() != null ? request.getPhone() : existing.getPhone())
                .location(request.getLocation() != null ? request.getLocation() : existing.getLocation())
                .bio(request.getBio() != null ? request.getBio() : existing.getBio())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(saved);

        // Audit log for profile update
        auditService.logEvent(saved.getId(), "PROFILE_UPDATE", "User", saved.getId(), "User profile updated", ipAddress, userAgent);
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
        for (RefreshToken token : userTokens) {
            if (passwordEncoder.matches(refreshTokenValue, token.getTokenHash())) {
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
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(newRefreshTokenValue))
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
    public void logout(UUID userId, String ipAddress, String userAgent) {
        // Revoke all refresh tokens for the user
        refreshTokenRepository.revokeAllTokensForUser(userId);

        // Audit log for logout
        auditService.logEvent(userId, "LOGOUT", "User", userId, "User logged out successfully", ipAddress, userAgent);
    }

    @Override
    public Optional<User> getProfile(UUID userId) {
        return userRepository.findById(userId);
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

}
package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.RefreshToken;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RefreshTokenRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImplementations implements UserUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserServiceImplementations(UserRepository userRepository,
                                    RefreshTokenRepository refreshTokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public User register(User user) {
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

        return userRepository.save(newUser);

    }

    @Override
    public Optional<AuthResponse> login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();

        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        if (!matches) {
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
        User updatedUser = User.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .userType(user.getUserType())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .mfaEnabled(user.isMfaEnabled())
                .mfaSecret(user.getMfaSecret())
                .googleId(user.getGoogleId())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .location(user.getLocation())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .updatedAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();

        userRepository.save(updatedUser);

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
    public void updateProfile(User user) {
        Optional<User> existingOpt = userRepository.findById(user.getId());

        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User existing = existingOpt.get();

        User saved = User.builder()
                .id(existing.getId())
                .firstName(user.getFirstName() != null ? user.getFirstName() : existing.getFirstName())
                .lastName(user.getLastName() != null ? user.getLastName() : existing.getLastName())
                .email(existing.getEmail())
                .passwordHash(existing.getPasswordHash())
                .userType(existing.getUserType())
                .status(existing.getStatus())
                .emailVerified(existing.isEmailVerified())
                .mfaEnabled(existing.isMfaEnabled())
                .mfaSecret(existing.getMfaSecret())
                .googleId(existing.getGoogleId())
                .avatarUrl(existing.getAvatarUrl())
                .phone(user.getPhone() != null ? user.getPhone() : existing.getPhone())
                .location(user.getLocation() != null ? user.getLocation() : existing.getLocation())
                .bio(user.getBio() != null ? user.getBio() : existing.getBio())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .lastLoginAt(existing.getLastLoginAt())
                .build();

        userRepository.save(saved);
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenValue) {
        // Find refresh token by hash
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenHash(
            passwordEncoder.encode(refreshTokenValue)
        );

        if (refreshTokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        // Check if token is expired or revoked
        if (refreshToken.getExpiresAt().isBefore(Instant.now()) || refreshToken.getRevokedAt() != null) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        // Get user
        Optional<User> userOpt = userRepository.findById(refreshToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();

        // Revoke old refresh token
        refreshTokenRepository.revokeToken(passwordEncoder.encode(refreshTokenValue));

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

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(900000L) // 15 minutes
                .user(user)
                .build();
    }

}


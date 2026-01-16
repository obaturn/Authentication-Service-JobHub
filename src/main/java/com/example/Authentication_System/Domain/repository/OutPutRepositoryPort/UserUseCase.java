package com.example.Authentication_System.Domain.repository.OutPutRepositoryPort;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.MfaSetupResponse;
import com.example.Authentication_System.Domain.model.ProfileUpdateRequest;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserUseCase {
    User register(User user, String ipAddress, String userAgent);
    Optional<AuthResponse> login(String email, String password, String ipAddress, String userAgent);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    void updateProfile(UUID userId, ProfileUpdateRequest request, String ipAddress, String userAgent);
    AuthResponse refreshToken(String refreshToken, String ipAddress, String userAgent);
    void logout(UUID userId, String accessToken, String ipAddress, String userAgent);
    Optional<UserProfile> getProfile(UUID userId);
    void deactivateAccount(UUID userId, String ipAddress, String userAgent);

    // Email Verification
    void sendVerificationEmail(String email);
    boolean verifyEmail(String token);

    // Password Reset
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);

    // MFA
    MfaSetupResponse setupMfa(UUID userId);
    void enableMfa(UUID userId, String code);
    Optional<AuthResponse> verifyMfa(String mfaToken, String code);
}
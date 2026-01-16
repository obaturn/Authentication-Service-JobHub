package com.example.Authentication_System.Controllers;

import com.example.Authentication_System.Domain.model.*;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import com.example.Authentication_System.Security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserUseCase userUseCase;
    private final JwtUtils jwtUtils;

    public AuthController(UserUseCase userUseCase, JwtUtils jwtUtils) {
        this.userUseCase = userUseCase;
        this.jwtUtils = jwtUtils;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private UUID getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userIdStr = jwtUtils.getUserIdFromToken(token);
            return UUID.fromString(userIdStr);
        }
        throw new IllegalArgumentException("Invalid or missing Authorization header");
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        User registeredUser = userUseCase.register(user, ipAddress, userAgent);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        Optional<AuthResponse> authResponse = userUseCase.login(loginRequest.getEmail(), loginRequest.getPassword(), ipAddress, userAgent);

        if (authResponse.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(authResponse.get());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletRequest request) {
        try {
            String ipAddress = getClientIp(request);
            String userAgent = getUserAgent(request);
            AuthResponse authResponse = userUseCase.refreshToken(refreshTokenRequest.getRefreshToken(), ipAddress, userAgent);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest profileUpdateRequest, HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        userUseCase.updateProfile(userId, profileUpdateRequest, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        userUseCase.logout(userId, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        Optional<UserProfile> userProfile = userUseCase.getProfile(userId);
        return userProfile.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deactivateAccount(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        userUseCase.deactivateAccount(userId, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-verification-email")
    public ResponseEntity<Void> sendVerificationEmail(@RequestBody Map<String, String> payload) {
        userUseCase.sendVerificationEmail(payload.get("email"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        boolean isVerified = userUseCase.verifyEmail(token);
        if (isVerified) {
            return ResponseEntity.ok("Email verified successfully.");
        }
        return ResponseEntity.badRequest().body("Invalid or expired verification token.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody Map<String, String> payload) {
        userUseCase.forgotPassword(payload.get("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> payload) {
        userUseCase.resetPassword(payload.get("token"), payload.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        return ResponseEntity.ok(userUseCase.setupMfa(userId));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<Void> enableMfa(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        userUseCase.enableMfa(userId, payload.get("code"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login/mfa")
    public ResponseEntity<AuthResponse> verifyMfa(@RequestBody Map<String, String> payload) {
        return userUseCase.verifyMfa(payload.get("mfaToken"), payload.get("code"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }
}
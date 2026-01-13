package com.example.Authentication_System.Controllers;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.LoginRequest;
import com.example.Authentication_System.Domain.model.ProfileUpdateRequest;
import com.example.Authentication_System.Domain.model.RefreshTokenRequest;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import com.example.Authentication_System.Security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
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

    @PostMapping("/auth/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        User registeredUser = userUseCase.register(user, ipAddress, userAgent);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        Optional<AuthResponse> authResponse = userUseCase.login(loginRequest.getEmail(), loginRequest.getPassword(), ipAddress, userAgent);

        if (authResponse.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(authResponse.get());
    }

    @PostMapping("/auth/refresh")
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

    @PutMapping("/auth/profile/{userId}")
    public ResponseEntity<Void> updateProfile(@PathVariable UUID userId, @Valid @RequestBody ProfileUpdateRequest profileUpdateRequest, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        userUseCase.updateProfile(userId, profileUpdateRequest, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        userUseCase.logout(userId, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/profile")
    public ResponseEntity<User> getProfile(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        Optional<User> user = userUseCase.getProfile(userId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user.get());
    }

    @DeleteMapping("/users/account")
    public ResponseEntity<Void> deactivateAccount(HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        userUseCase.deactivateAccount(userId, ipAddress, userAgent);
        return ResponseEntity.ok().build();
    }
}
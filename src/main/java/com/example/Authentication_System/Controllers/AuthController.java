package com.example.Authentication_System.Controllers;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.LoginRequest;
import com.example.Authentication_System.Domain.model.RefreshTokenRequest;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserUseCase userUseCase;

    public AuthController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User registeredUser = userUseCase.register(user);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<AuthResponse> authResponse = userUseCase.login(loginRequest.getEmail(), loginRequest.getPassword());

        if (authResponse.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(authResponse.get());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            AuthResponse authResponse = userUseCase.refreshToken(refreshTokenRequest.getRefreshToken());
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
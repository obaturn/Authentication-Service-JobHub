package com.example.Authentication_System.Domain.repository.OutPutRepositoryPort;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserUseCase {
    User register(User user);
    Optional<AuthResponse> login(String email, String password);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    void updateProfile(User user);
    AuthResponse refreshToken(String refreshToken);
}

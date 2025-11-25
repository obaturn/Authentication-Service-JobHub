package com.example.Authentication_System.Domain.repository.OutPutRepositoryPort;

import com.example.Authentication_System.Domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserUseCase {
    User register(User user);
    Optional<User> login(String email, String password);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    void updateProfile(User user);
}

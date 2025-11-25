package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
}

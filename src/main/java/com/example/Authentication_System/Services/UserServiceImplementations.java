package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class UserServiceImplementations implements UserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImplementations(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(User user) {
        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }


        String hashedPassword = passwordEncoder.encode(user.getPassword());


        Instant now = Instant.now();

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .password(hashedPassword)
                .role(user.getRole() != null ? user.getRole() : "USER")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return userRepository.save(newUser);

    }

    @Override
    public Optional<User> login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();


        boolean matches = passwordEncoder.matches(password, user.getPassword());
        if (!matches) {
            return Optional.empty();
        }

        return Optional.of(user);
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
                .fullName(user.getFullName() != null ? user.getFullName() : existing.getFullName())
                .email(existing.getEmail())
                .password(existing.getPassword())
                .role(existing.getRole())
                .isActive(existing.isActive())
                .createdAt(existing.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(saved);
    }

    }


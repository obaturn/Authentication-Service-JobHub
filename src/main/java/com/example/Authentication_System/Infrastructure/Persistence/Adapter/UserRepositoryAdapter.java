package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.UserJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryAdapter.class);

    private final UserJpaRepository userJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = UserMapper.toEntity(user);
        logger.info("USER_REPO: Saving user {} with token {}", user.getEmail(), user.getEmailVerificationToken());
        UserEntity savedEntity = userJpaRepository.save(userEntity);
        userJpaRepository.flush();
        User savedDomain = UserMapper.toDomain(savedEntity);
        logger.info("USER_REPO: Saved user {} with token {}", savedDomain.getEmail(), savedDomain.getEmailVerificationToken());
        return savedDomain;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(entity -> UserMapper.toDomain(entity));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id)
                .map(entity -> UserMapper.toDomain(entity));
    }

    @Override
    public Optional<User> findByEmailVerificationToken(String token) {
        logger.info("USER_REPO: Searching for user with token: {}", token);
        Optional<UserEntity> entityOpt = userJpaRepository.findByEmailVerificationToken(token);
        if (entityOpt.isPresent()) {
            logger.info("USER_REPO: Found user {} with token {}", entityOpt.get().getEmail(), token);
        } else {
            logger.warn("USER_REPO: No user found with token: {}", token);
        }
        return entityOpt.map(entity -> UserMapper.toDomain(entity));
    }

    @Override
    public Optional<User> findByPasswordResetToken(String token) {
        return userJpaRepository.findByPasswordResetToken(token)
                .map(entity -> UserMapper.toDomain(entity));
    }
}
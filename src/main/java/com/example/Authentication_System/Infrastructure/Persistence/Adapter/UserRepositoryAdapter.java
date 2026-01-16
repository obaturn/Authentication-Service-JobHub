package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.UserJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = userMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(userEntity);
        userJpaRepository.flush();
        return userMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailVerificationToken(String token) {
        return userJpaRepository.findByEmailVerificationToken(token)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByPasswordResetToken(String token) {
        return userJpaRepository.findByPasswordResetToken(token)
                .map(userMapper::toDomain);
    }
}

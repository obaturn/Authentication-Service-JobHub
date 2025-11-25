package com.example.Authentication_System.Infrastructure.Persistence.Adapter;


import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.UserDataJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserMapper;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;


import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserDataJpaRepository jpa;

    public UserRepositoryAdapter(UserDataJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpa.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id)
                .map(UserMapper::toDomain);
    }
}

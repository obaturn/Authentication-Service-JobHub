package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.UserRole;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRoleRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.UserRoleDataJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserMapper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserRoleRepositoryAdapter implements UserRoleRepository {

    private final UserRoleDataJpaRepository jpa;

    public UserRoleRepositoryAdapter(UserRoleDataJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public UserRole save(UserRole userRole) {
        var saved = jpa.save(UserMapper.toEntity(userRole));
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<UserRole> findById(UUID id) {
        return jpa.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public List<UserRole> findByUserId(UUID userId) {
        return jpa.findByUserId(userId)
                .stream()
                .map(UserMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId) {
        return jpa.findByUserIdAndRoleId(userId, roleId)
                .map(UserMapper::toDomain);
    }
}
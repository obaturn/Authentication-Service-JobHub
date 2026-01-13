package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.Role;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RoleRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.RoleDataJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserMapper;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleDataJpaRepository jpa;

    public RoleRepositoryAdapter(RoleDataJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Role save(Role role) {
        var saved = jpa.save(UserMapper.toEntity(role));
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return jpa.findById(id)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpa.findByName(name)
                .map(UserMapper::toDomain);
    }
}
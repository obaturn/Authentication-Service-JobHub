package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.UserProfile;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserProfileRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserProfileEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.UserProfileJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserProfileMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserProfileRepositoryAdapter implements UserProfileRepository {

    private final UserProfileJpaRepository userProfileJpaRepository;
    private final UserProfileMapper userProfileMapper;

    public UserProfileRepositoryAdapter(UserProfileJpaRepository userProfileJpaRepository, UserProfileMapper userProfileMapper) {
        this.userProfileJpaRepository = userProfileJpaRepository;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserProfileEntity userProfileEntity = userProfileMapper.toEntity(userProfile);
        UserProfileEntity savedEntity = userProfileJpaRepository.save(userProfileEntity);
        return userProfileMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<UserProfile> findByUserId(UUID userId) {
        return userProfileJpaRepository.findByUserId(userId)
                .map(userProfileMapper::toDomain);
    }
}

package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.RefreshToken;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RefreshTokenRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.RefreshTokenEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.RefreshTokenDataJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenDataJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenDataJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = mapToEntity(refreshToken);
        RefreshTokenEntity saved = jpa.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash)
                .map(this::mapToDomain);
    }

    @Override
    public Optional<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId) {
        return jpa.findByUserIdAndRevokedAtIsNull(userId)
                .map(this::mapToDomain);
    }

    @Override
    public List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(UUID userId) {
        return jpa.findAllByUserIdAndRevokedAtIsNull(userId)
                .stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void revokeToken(String tokenHash) {
        Optional<RefreshTokenEntity> entityOpt = jpa.findByTokenHash(tokenHash);
        if (entityOpt.isPresent()) {
            RefreshTokenEntity entity = entityOpt.get();
            entity.setRevokedAt(java.time.LocalDateTime.now());
            jpa.save(entity);
        }
    }

    @Override
    public void revokeAllTokensForUser(UUID userId) {
        jpa.revokeAllTokensForUser(userId);
    }

    private RefreshTokenEntity mapToEntity(RefreshToken refreshToken) {
        return RefreshTokenEntity.builder()
                .id(refreshToken.getId())
                .userId(refreshToken.getUserId())
                .tokenHash(refreshToken.getTokenHash())
                .expiresAt(refreshToken.getExpiresAt() != null ?
                        java.time.LocalDateTime.ofInstant(refreshToken.getExpiresAt(),
                                java.time.ZoneId.systemDefault()) : null)
                .createdAt(refreshToken.getCreatedAt() != null ?
                        java.time.LocalDateTime.ofInstant(refreshToken.getCreatedAt(),
                                java.time.ZoneId.systemDefault()) : null)
                .revokedAt(refreshToken.getRevokedAt() != null ?
                        java.time.LocalDateTime.ofInstant(refreshToken.getRevokedAt(),
                                java.time.ZoneId.systemDefault()) : null)
                .build();
    }

    private RefreshToken mapToDomain(RefreshTokenEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt() != null ?
                        entity.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .revokedAt(entity.getRevokedAt() != null ?
                        entity.getRevokedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build();
    }
}
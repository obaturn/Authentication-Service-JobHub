package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(UUID userId);
    void revokeToken(String tokenHash);
    void revokeAllTokensForUser(UUID userId);
}
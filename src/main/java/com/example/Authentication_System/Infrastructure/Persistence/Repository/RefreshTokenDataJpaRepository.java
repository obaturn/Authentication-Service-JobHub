package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenDataJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    // This method returns a single Optional, but there could be multiple active tokens.
    // It's safer to rely on the List version below or rename this if strict uniqueness is expected.
    // For now, I'll leave it as is to avoid breaking other code, but be aware.
    Optional<RefreshTokenEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    // This is the correct method for fetching all active tokens
    List<RefreshTokenEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = CURRENT_TIMESTAMP WHERE r.userId = :userId AND r.revokedAt IS NULL")
    void revokeAllTokensForUser(@Param("userId") UUID userId);
}
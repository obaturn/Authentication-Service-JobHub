package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByUserId(UUID userId);
}

package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByEmailVerificationToken(String emailVerificationToken);
    Optional<UserEntity> findByPasswordResetToken(String passwordResetToken);
}

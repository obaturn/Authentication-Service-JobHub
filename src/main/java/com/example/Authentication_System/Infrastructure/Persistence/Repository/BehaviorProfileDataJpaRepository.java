package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.BehaviorProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BehaviorProfileDataJpaRepository extends JpaRepository<BehaviorProfileEntity, Long> {
    Optional<BehaviorProfileEntity> findByUserId(UUID userId);
}
package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.ExperienceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExperienceJpaRepository extends JpaRepository<ExperienceEntity, UUID> {
    List<ExperienceEntity> findByUserId(UUID userId);
    void deleteByUserIdAndId(UUID userId, UUID id);
}

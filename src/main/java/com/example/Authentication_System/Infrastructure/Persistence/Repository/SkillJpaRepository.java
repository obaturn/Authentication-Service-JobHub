package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillJpaRepository extends JpaRepository<SkillEntity, UUID> {
    List<SkillEntity> findByUserId(UUID userId);
    void deleteByUserIdAndId(UUID userId, UUID id);
}

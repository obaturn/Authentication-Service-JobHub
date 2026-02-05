package com.example.Authentication_System.Infrastructure.Persistence.Repository;

import com.example.Authentication_System.Infrastructure.Persistence.Entity.EducationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EducationJpaRepository extends JpaRepository<EducationEntity, UUID> {
    List<EducationEntity> findByUserId(UUID userId);
    void deleteByUserIdAndId(UUID userId, UUID id);
}

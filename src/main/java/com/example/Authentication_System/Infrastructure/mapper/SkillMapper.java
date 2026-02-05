package com.example.Authentication_System.Infrastructure.mapper;

import com.example.Authentication_System.Domain.model.Skill;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.SkillEntity;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {

    public Skill toDomain(SkillEntity entity) {
        if (entity == null) {
            return null;
        }
        return Skill.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .category(entity.getCategory())
                .proficiencyLevel(entity.getProficiencyLevel())
                .yearsOfExperience(entity.getYearsOfExperience())
                .isVerified(entity.isVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SkillEntity toEntity(Skill domain) {
        if (domain == null) {
            return null;
        }
        return SkillEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .name(domain.getName())
                .category(domain.getCategory())
                .proficiencyLevel(domain.getProficiencyLevel())
                .yearsOfExperience(domain.getYearsOfExperience())
                .isVerified(domain.isVerified())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

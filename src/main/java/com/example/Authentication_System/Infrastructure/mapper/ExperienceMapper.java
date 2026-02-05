package com.example.Authentication_System.Infrastructure.mapper;

import com.example.Authentication_System.Domain.model.Experience;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.ExperienceEntity;
import org.springframework.stereotype.Component;

@Component
public class ExperienceMapper {

    public Experience toDomain(ExperienceEntity entity) {
        if (entity == null) {
            return null;
        }
        return Experience.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .companyName(entity.getCompanyName())
                .jobTitle(entity.getJobTitle())
                .location(entity.getLocation())
                .isRemote(entity.isRemote())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isCurrentPosition(entity.isCurrentPosition())
                .description(entity.getDescription())
                .employmentType(entity.getEmploymentType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ExperienceEntity toEntity(Experience domain) {
        if (domain == null) {
            return null;
        }
        return ExperienceEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .companyName(domain.getCompanyName())
                .jobTitle(domain.getJobTitle())
                .location(domain.getLocation())
                .isRemote(domain.isRemote())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .isCurrentPosition(domain.isCurrentPosition())
                .description(domain.getDescription())
                .employmentType(domain.getEmploymentType())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

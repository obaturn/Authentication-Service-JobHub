package com.example.Authentication_System.Infrastructure.mapper;

import com.example.Authentication_System.Domain.model.Education;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.EducationEntity;
import org.springframework.stereotype.Component;

@Component
public class EducationMapper {

    public Education toDomain(EducationEntity entity) {
        if (entity == null) {
            return null;
        }
        return Education.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .institutionName(entity.getInstitutionName())
                .degree(entity.getDegree())
                .fieldOfStudy(entity.getFieldOfStudy())
                .location(entity.getLocation())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isCurrent(entity.isCurrent())
                .description(entity.getDescription())
                .gpa(entity.getGpa())
                .honors(entity.getHonors())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public EducationEntity toEntity(Education domain) {
        if (domain == null) {
            return null;
        }
        return EducationEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .institutionName(domain.getInstitutionName())
                .degree(domain.getDegree())
                .fieldOfStudy(domain.getFieldOfStudy())
                .location(domain.getLocation())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .isCurrent(domain.isCurrent())
                .description(domain.getDescription())
                .gpa(domain.getGpa())
                .honors(domain.getHonors())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

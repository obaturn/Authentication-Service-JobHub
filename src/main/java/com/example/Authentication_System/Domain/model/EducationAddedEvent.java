package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user adds a new education
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationAddedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // EDUCATION_ADDED
    private String entityType;       // EDUCATION
    private UUID entityId;          // Education ID
    private String entityName;       // Degree + Field of Study (primary identifier)
    private String institutionName;
    private String degree;
    private String fieldOfStudy;
    private String location;
    private Instant startDate;
    private Instant endDate;
    private boolean isCurrent;
    private Double gpa;
    private Instant timestamp;
    private String correlationId;

    public static EducationAddedEvent fromEducation(Education education, UUID userId, String correlationId) {
        return EducationAddedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .eventType("EDUCATION_ADDED")
                .entityType("EDUCATION")
                .entityId(education.getId())
                .entityName(education.getDegree() + " in " + education.getFieldOfStudy())
                .institutionName(education.getInstitutionName())
                .degree(education.getDegree())
                .fieldOfStudy(education.getFieldOfStudy())
                .location(education.getLocation())
                .startDate(education.getStartDate())
                .endDate(education.getEndDate())
                .isCurrent(education.isCurrent())
                .gpa(education.getGpa())
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
    }
}

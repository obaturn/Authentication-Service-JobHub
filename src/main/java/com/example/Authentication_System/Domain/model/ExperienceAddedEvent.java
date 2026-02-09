package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user adds a new experience
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceAddedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // EXPERIENCE_ADDED
    private String entityType;       // EXPERIENCE
    private UUID entityId;          // Experience ID
    private String entityName;       // Job title (primary identifier)
    private String companyName;
    private String jobTitle;
    private String location;
    private boolean isRemote;
    private Instant startDate;
    private Instant endDate;
    private boolean isCurrentPosition;
    private String employmentType;
    private Instant timestamp;
    private String correlationId;

    public static ExperienceAddedEvent fromExperience(Experience experience, UUID userId, String correlationId) {
        return ExperienceAddedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .eventType("EXPERIENCE_ADDED")
                .entityType("EXPERIENCE")
                .entityId(experience.getId())
                .entityName(experience.getJobTitle())
                .companyName(experience.getCompanyName())
                .jobTitle(experience.getJobTitle())
                .location(experience.getLocation())
                .isRemote(experience.isRemote())
                .startDate(experience.getStartDate())
                .endDate(experience.getEndDate())
                .isCurrentPosition(experience.isCurrentPosition())
                .employmentType(experience.getEmploymentType())
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
    }
}

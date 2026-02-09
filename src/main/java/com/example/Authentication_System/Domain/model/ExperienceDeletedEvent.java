package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user deletes an experience
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDeletedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // EXPERIENCE_DELETED
    private String entityType;       // EXPERIENCE
    private UUID entityId;          // Experience ID (before deletion)
    private String entityName;       // Job title (before deletion)
    private String companyName;
    private Instant timestamp;
    private String correlationId;

    public static ExperienceDeletedEvent create(UUID experienceId, String jobTitle, String companyName, UUID userId, String correlationId) {
        return ExperienceDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .eventType("EXPERIENCE_DELETED")
                .entityType("EXPERIENCE")
                .entityId(experienceId)
                .entityName(jobTitle)
                .companyName(companyName)
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
    }
}

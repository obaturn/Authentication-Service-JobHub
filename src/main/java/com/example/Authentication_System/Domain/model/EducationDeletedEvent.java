package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user deletes an education
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationDeletedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // EDUCATION_DELETED
    private String entityType;       // EDUCATION
    private UUID entityId;          // Education ID (before deletion)
    private String entityName;       // Degree + Field of Study (before deletion)
    private String institutionName;
    private Instant timestamp;
    private String correlationId;

    public static EducationDeletedEvent create(UUID educationId, String degree, String fieldOfStudy, String institutionName, UUID userId, String correlationId) {
        return EducationDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .eventType("EDUCATION_DELETED")
                .entityType("EDUCATION")
                .entityId(educationId)
                .entityName(degree + " in " + fieldOfStudy)
                .institutionName(institutionName)
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
    }
}

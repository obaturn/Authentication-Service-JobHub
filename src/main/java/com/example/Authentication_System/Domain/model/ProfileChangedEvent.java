package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Base event for profile changes (Skills, Experience, Education)
 * Published to Kafka when user modifies their profile data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileChangedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // ADDED, UPDATED, DELETED
    private String entityType;       // SKILL, EXPERIENCE, EDUCATION
    private UUID entityId;
    private String entityName;
    private Instant timestamp;
    private String correlationId;

    // Getters for convenience
    public UUID getEventId() {
        return eventId != null ? eventId : UUID.randomUUID();
    }

    public Instant getTimestamp() {
        return timestamp != null ? timestamp : Instant.now();
    }
}

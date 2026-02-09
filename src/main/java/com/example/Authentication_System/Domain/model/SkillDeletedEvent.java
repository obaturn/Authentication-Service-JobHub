package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user deletes a skill
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDeletedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // SKILL_DELETED
    private String entityType;       // SKILL
    private UUID entityId;          // Skill ID (before deletion)
    private String entityName;       // Skill name (before deletion)
    private Instant timestamp;
    private String correlationId;

    public static SkillDeletedEvent create(UUID skillId, String skillName, UUID userId, String correlationId) {
        return SkillDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .eventType("SKILL_DELETED")
                .entityType("SKILL")
                .entityId(skillId)
                .entityName(skillName)
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
    }
}

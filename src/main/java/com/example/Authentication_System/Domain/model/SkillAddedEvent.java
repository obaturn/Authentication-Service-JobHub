package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user adds a new skill
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAddedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // SKILL_ADDED
    private String entityType;       // SKILL
    private UUID entityId;          // Skill ID
    private String entityName;       // Skill name
    private String category;        // Skill category
    private String proficiencyLevel;// Skill proficiency level
    private Integer yearsOfExperience;
    private Instant timestamp;
    private String correlationId;

    public static SkillAddedEvent fromSkill(Skill skill, UUID userId, String correlationId) {
        return SkillAddedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .eventType("SKILL_ADDED")
                .entityType("SKILL")
                .entityId(skill.getId())
                .entityName(skill.getName())
                .category(skill.getCategory())
                .proficiencyLevel(skill.getProficiencyLevel())
                .yearsOfExperience(skill.getYearsOfExperience())
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
    }
}

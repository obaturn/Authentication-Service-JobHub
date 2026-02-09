package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user updates a skill
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillUpdatedEvent {

    private UUID eventId;
    private UUID userId;
    private String eventType;        // SKILL_UPDATED
    private String entityType;       // SKILL
    private UUID entityId;          // Skill ID
    private String entityName;       // Skill name
    private String category;        // Updated skill category
    private String proficiencyLevel;// Updated proficiency level
    private Integer yearsOfExperience;
    private Instant timestamp;
    private String correlationId;

    public static SkillUpdatedEvent fromSkill(Skill skill, UUID userId, String correlationId) {
        return SkillUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .eventType("SKILL_UPDATED")
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

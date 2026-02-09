package com.example.Authentication_System.Infrastructure.Adapter;

import com.example.Authentication_System.Domain.model.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka event publisher for all authentication and profile events
 */
@Component
public class KafkaEventPublisher {

    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String PROFILE_CHANGES_TOPIC = "profile-changes";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // ==================== User Events ====================

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        kafkaTemplate.send(USER_EVENTS_TOPIC, event.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("Failed to publish UserRegisteredEvent: " + ex.getMessage());
                    } else {
                        System.out.println("UserRegisteredEvent published successfully: " + event.getUserId());
                    }
                });
    }

    // ==================== Profile Change Events ====================

    /**
     * Generic method to publish profile change events
     * @param event The event to publish (SkillAddedEvent, SkillUpdatedEvent, SkillDeletedEvent, etc.)
     * @return CompletableFuture with the send result
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<SendResult<String, Object>> publishProfileChangedEvent(Object event) {
        String userId = extractUserId(event);
        return kafkaTemplate.send(PROFILE_CHANGES_TOPIC, userId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("Failed to publish profile change event: " + event.getClass().getSimpleName() + " - " + ex.getMessage());
                    } else {
                        System.out.println("Profile change event published successfully: " + event.getClass().getSimpleName() + " for user: " + userId);
                    }
                });
    }

    /**
     * Extract user ID from various event types
     */
    private String extractUserId(Object event) {
        if (event instanceof SkillAddedEvent) {
            return ((SkillAddedEvent) event).getUserId().toString();
        } else if (event instanceof SkillUpdatedEvent) {
            return ((SkillUpdatedEvent) event).getUserId().toString();
        } else if (event instanceof SkillDeletedEvent) {
            return ((SkillDeletedEvent) event).getUserId().toString();
        } else if (event instanceof ExperienceAddedEvent) {
            return ((ExperienceAddedEvent) event).getUserId().toString();
        } else if (event instanceof ExperienceUpdatedEvent) {
            return ((ExperienceUpdatedEvent) event).getUserId().toString();
        } else if (event instanceof ExperienceDeletedEvent) {
            return ((ExperienceDeletedEvent) event).getUserId().toString();
        } else if (event instanceof EducationAddedEvent) {
            return ((EducationAddedEvent) event).getUserId().toString();
        } else if (event instanceof EducationUpdatedEvent) {
            return ((EducationUpdatedEvent) event).getUserId().toString();
        } else if (event instanceof EducationDeletedEvent) {
            return ((EducationDeletedEvent) event).getUserId().toString();
        } else if (event instanceof ProfileChangedEvent) {
            return ((ProfileChangedEvent) event).getUserId().toString();
        }
        return "unknown";
    }

    // ==================== Skill Events ====================

    public void publishSkillAddedEvent(SkillAddedEvent event) {
        publishProfileChangedEvent(event);
    }

    public void publishSkillUpdatedEvent(SkillUpdatedEvent event) {
        publishProfileChangedEvent(event);
    }

    public void publishSkillDeletedEvent(SkillDeletedEvent event) {
        publishProfileChangedEvent(event);
    }

    // ==================== Experience Events ====================

    public void publishExperienceAddedEvent(ExperienceAddedEvent event) {
        publishProfileChangedEvent(event);
    }

    public void publishExperienceUpdatedEvent(ExperienceUpdatedEvent event) {
        publishProfileChangedEvent(event);
    }

    public void publishExperienceDeletedEvent(ExperienceDeletedEvent event) {
        publishProfileChangedEvent(event);
    }

    // ==================== Education Events ====================

    public void publishEducationAddedEvent(EducationAddedEvent event) {
        publishProfileChangedEvent(event);
    }

    public void publishEducationUpdatedEvent(EducationUpdatedEvent event) {
        publishProfileChangedEvent(event);
    }

    public void publishEducationDeletedEvent(EducationDeletedEvent event) {
        publishProfileChangedEvent(event);
    }
}

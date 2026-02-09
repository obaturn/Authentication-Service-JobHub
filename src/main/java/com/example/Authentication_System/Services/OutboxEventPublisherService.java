package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.*;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.OutboxEventRepository;
import com.example.Authentication_System.Infrastructure.Adapter.KafkaEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for publishing events to Kafka via the outbox pattern
 * Ensures reliable event delivery even if Kafka is temporarily unavailable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();

        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
                outboxEventRepository.updateStatus(event.getId(), "PUBLISHED", Instant.now(), null);
                log.info("Successfully published outbox event: {} - {}", event.getEventType(), event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {} - {}", event.getEventType(), event.getId(), e);
                handlePublishFailure(event, e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findEventsForRetry(3); // Max 3 retries

        for (OutboxEvent event : failedEvents) {
            try {
                publishEvent(event);
                outboxEventRepository.updateStatus(event.getId(), "PUBLISHED", Instant.now(), null);
                log.info("Successfully retried and published outbox event: {} - {}", event.getEventType(), event.getId());
            } catch (Exception e) {
                log.error("Failed to retry outbox event: {} - {}", event.getEventType(), event.getId(), e);
                outboxEventRepository.updateStatus(event.getId(), "FAILED", null, e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void publishEvent(OutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case "UserRegisteredEvent":
            case "UserRegistered":
            case "VerificationEmailRequested":
                var userEvent = objectMapper.readValue(event.getPayload(), UserRegisteredEvent.class);
                kafkaEventPublisher.publishUserRegisteredEvent(userEvent);
                break;

            // ==================== Skill Events ====================
            case "SkillAddedEvent":
                var skillAddedEvent = objectMapper.readValue(event.getPayload(), SkillAddedEvent.class);
                kafkaEventPublisher.publishSkillAddedEvent(skillAddedEvent);
                break;

            case "SkillUpdatedEvent":
                var skillUpdatedEvent = objectMapper.readValue(event.getPayload(), SkillUpdatedEvent.class);
                kafkaEventPublisher.publishSkillUpdatedEvent(skillUpdatedEvent);
                break;

            case "SkillDeletedEvent":
                var skillDeletedEvent = objectMapper.readValue(event.getPayload(), SkillDeletedEvent.class);
                kafkaEventPublisher.publishSkillDeletedEvent(skillDeletedEvent);
                break;

            // ==================== Experience Events ====================
            case "ExperienceAddedEvent":
                var experienceAddedEvent = objectMapper.readValue(event.getPayload(), ExperienceAddedEvent.class);
                kafkaEventPublisher.publishExperienceAddedEvent(experienceAddedEvent);
                break;

            case "ExperienceUpdatedEvent":
                var experienceUpdatedEvent = objectMapper.readValue(event.getPayload(), ExperienceUpdatedEvent.class);
                kafkaEventPublisher.publishExperienceUpdatedEvent(experienceUpdatedEvent);
                break;

            case "ExperienceDeletedEvent":
                var experienceDeletedEvent = objectMapper.readValue(event.getPayload(), ExperienceDeletedEvent.class);
                kafkaEventPublisher.publishExperienceDeletedEvent(experienceDeletedEvent);
                break;

            // ==================== Education Events ====================
            case "EducationAddedEvent":
                var educationAddedEvent = objectMapper.readValue(event.getPayload(), EducationAddedEvent.class);
                kafkaEventPublisher.publishEducationAddedEvent(educationAddedEvent);
                break;

            case "EducationUpdatedEvent":
                var educationUpdatedEvent = objectMapper.readValue(event.getPayload(), EducationUpdatedEvent.class);
                kafkaEventPublisher.publishEducationUpdatedEvent(educationUpdatedEvent);
                break;

            case "EducationDeletedEvent":
                var educationDeletedEvent = objectMapper.readValue(event.getPayload(), EducationDeletedEvent.class);
                kafkaEventPublisher.publishEducationDeletedEvent(educationDeletedEvent);
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }

    private void handlePublishFailure(OutboxEvent event, String error) {
        // Update retry count and status
        int newRetryCount = event.getRetryCount() + 1;
        if (newRetryCount >= 3) {
            outboxEventRepository.updateStatus(event.getId(), "FAILED", null, error);
            log.warn("Event {} moved to FAILED status after {} retries", event.getId(), newRetryCount);
        } else {
            // Just log for now - the scheduled retry will handle it
            log.warn("Event {} failed (retry {}), will retry later", event.getId(), newRetryCount);
        }
    }
}

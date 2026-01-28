package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.OutboxEvent;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.OutboxEventRepository;
import com.example.Authentication_System.Infrastructure.Adapter.KafkaEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

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
                log.info("Successfully published outbox event: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
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
                log.info("Successfully retried and published outbox event: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to retry outbox event: {}", event.getId(), e);
                outboxEventRepository.updateStatus(event.getId(), "FAILED", null, e.getMessage());
            }
        }
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case "UserRegisteredEvent": // Updated to match class name
            case "UserRegistered": // Keep for backward compatibility if needed
            case "VerificationEmailRequested":
                // Deserialize and publish
                var userEvent = objectMapper.readValue(event.getPayload(), com.example.Authentication_System.Domain.model.UserRegisteredEvent.class);
                kafkaEventPublisher.publishUserRegisteredEvent(userEvent);
                break;
            // Add other event types here as needed
            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }

    private void handlePublishFailure(OutboxEvent event, String error) {
        // For now, just update status. In production, you might want to implement
        // exponential backoff, dead letter queues, or alerting
        outboxEventRepository.updateStatus(event.getId(), "FAILED", null, error);
    }
}
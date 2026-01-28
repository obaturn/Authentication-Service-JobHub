package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findPendingEvents();
    List<OutboxEvent> findEventsForRetry(int maxRetries);
    void updateStatus(UUID eventId, String status, Instant publishedAt, String error);
    long countPendingEvents();
}
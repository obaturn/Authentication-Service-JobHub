package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.OutboxEvent;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.OutboxEventRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.OutboxEventEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.OutboxEventDataJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventDataJpaRepository jpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = toEntity(event);
        OutboxEventEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<OutboxEvent> findPendingEvents() {
        return jpaRepository.findByStatusOrderByCreatedAtAsc("PENDING")
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxEvent> findEventsForRetry(int maxRetries) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return jpaRepository.findPendingEventsForRetry("FAILED", fiveMinutesAgo, maxRetries)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(UUID eventId, String status, Instant publishedAt, String error) {
        OutboxEventEntity entity = jpaRepository.findById(eventId).orElseThrow();
        entity.setStatus(status);
        entity.setPublishedAt(publishedAt != null ? LocalDateTime.ofInstant(publishedAt, ZoneOffset.UTC) : null);
        entity.setLastError(error);
        if ("FAILED".equals(status)) {
            entity.setRetryCount(entity.getRetryCount() + 1);
        }
        jpaRepository.save(entity);
    }

    @Override
    public long countPendingEvents() {
        return jpaRepository.countByStatus("PENDING");
    }

    private OutboxEventEntity toEntity(OutboxEvent domain) {
        return OutboxEventEntity.builder()
                .id(domain.getId())
                .eventType(domain.getEventType())
                .payload(domain.getPayload())
                .topic(domain.getTopic())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt() != null ? LocalDateTime.ofInstant(domain.getCreatedAt(), ZoneOffset.UTC) : null)
                .publishedAt(domain.getPublishedAt() != null ? LocalDateTime.ofInstant(domain.getPublishedAt(), ZoneOffset.UTC) : null)
                .retryCount(domain.getRetryCount())
                .lastError(domain.getLastError())
                .build();
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return OutboxEvent.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .payload(entity.getPayload())
                .topic(entity.getTopic())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant(ZoneOffset.UTC) : null)
                .publishedAt(entity.getPublishedAt() != null ? entity.getPublishedAt().toInstant(ZoneOffset.UTC) : null)
                .retryCount(entity.getRetryCount())
                .lastError(entity.getLastError())
                .build();
    }
}
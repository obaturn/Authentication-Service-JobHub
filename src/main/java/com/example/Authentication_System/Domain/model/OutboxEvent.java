package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    private UUID id;
    private String eventType;
    private String payload;
    private String topic;
    private String status; // PENDING, PUBLISHED, FAILED
    private Instant createdAt;
    private Instant publishedAt;
    private int retryCount;
    private String lastError;
}
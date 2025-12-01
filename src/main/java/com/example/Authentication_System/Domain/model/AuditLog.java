package com.example.Authentication_System.Domain.model;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    private UUID id;
    private UUID userId;
    private String action;
    private String resource;
    private UUID resourceId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}
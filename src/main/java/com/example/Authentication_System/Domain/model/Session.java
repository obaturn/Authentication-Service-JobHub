package com.example.Authentication_System.Domain.model;

import lombok.*;
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Session {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private InetAddress ipAddress;
    private String userAgent;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant revokedAt;
}
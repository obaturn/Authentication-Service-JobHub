package com.example.Authentication_System.Domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRole {

    private UUID id;
    private UUID userId;
    private UUID roleId;
    private Instant assignedAt;
    private UUID assignedBy;
}
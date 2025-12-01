package com.example.Authentication_System.Domain.model;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permission {

    private UUID id;
    private String name; // e.g., "view_jobs", "post_jobs", "manage_users"
    private String description;
    private String resource; // e.g., "jobs", "users"
    private String action; // e.g., "read", "write", "delete"
    private Instant createdAt;
}
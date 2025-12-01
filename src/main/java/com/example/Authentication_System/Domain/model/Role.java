package com.example.Authentication_System.Domain.model;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    private UUID id;
    private String name; // job_seeker, employer, admin
    private String description;
    private Instant createdAt;
}
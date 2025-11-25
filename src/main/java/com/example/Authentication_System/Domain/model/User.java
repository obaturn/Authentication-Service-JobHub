package com.example.Authentication_System.Domain.model;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private UUID id;
    private String fullName;
    private String email;
    private String password;
    private String role;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}

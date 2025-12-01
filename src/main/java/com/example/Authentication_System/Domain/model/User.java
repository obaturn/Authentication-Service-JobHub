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
    private String email;
    private String passwordHash; // Renamed from password
    private String firstName;
    private String lastName;
    private String userType; // job_seeker, employer, admin
    private String status; // active, suspended, banned
    private boolean emailVerified;
    private boolean mfaEnabled;
    private String mfaSecret;
    private String googleId;
    private String avatarUrl;
    private String phone;
    private String location;
    private String bio;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
}

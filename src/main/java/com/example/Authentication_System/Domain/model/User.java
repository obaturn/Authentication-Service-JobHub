package com.example.Authentication_System.Domain.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private UUID id;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @ValidPassword
    @NotBlank(message = "Password is required")
    private String passwordHash; // Used for input password during registration

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @NotBlank(message = "User type is required")
    @Pattern(regexp = "job_seeker|employer|admin", message = "User type must be one of: job_seeker, employer, admin")
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
package com.example.Authentication_System.Domain.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "userProfile")
@ToString(exclude = "userProfile")
public class User {

    private UUID id;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @ValidPassword
    @NotBlank(message = "Password is required")
    private String passwordHash;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @NotBlank(message = "User type is required")
    @Pattern(regexp = "job_seeker|employer|admin", message = "User type must be one of: job_seeker, employer, admin")
    private String userType;

    private String status;

    @Builder.Default
    private boolean emailVerified = false;

    private String emailVerificationToken;

    private Instant emailVerificationExpiresAt;

    private String passwordResetToken;

    private Instant passwordResetExpiresAt;

    @Builder.Default
    private boolean mfaEnabled = false;

    private String mfaSecret;

    private String googleId;

    private UserProfile userProfile;

    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    private Instant createdAt;

    private Instant updatedAt;

    private Instant lastLoginAt;

    private int failedLoginAttempts;
    private Instant accountLockedUntil;
    private Instant lastFailedAttemptAt;

    // Helper method to link profile to user
    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        userProfile.setUser(this);
    }
}
package com.example.Authentication_System.Domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "user")
public class UserProfile {

    private UUID id;

    @JsonIgnore
    private User user;

    private String headline;

    private String bio;

    private String location;

    private String avatarUrl;

    private String websiteUrl;

    private String portfolioUrl;

    private String phone;

    private Instant createdAt;

    private Instant updatedAt;
}
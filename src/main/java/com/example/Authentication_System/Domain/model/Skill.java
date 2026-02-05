package com.example.Authentication_System.Domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Skill {

    private UUID id;

    @JsonIgnore
    private UUID userId;

    private String name;

    private String category;

    private String proficiencyLevel;

    private Integer yearsOfExperience;

    private boolean isVerified;

    private Instant createdAt;

    private Instant updatedAt;
}

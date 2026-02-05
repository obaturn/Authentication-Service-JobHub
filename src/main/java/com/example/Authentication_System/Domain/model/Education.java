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
public class Education {

    private UUID id;

    @JsonIgnore
    private UUID userId;

    private String institutionName;

    private String degree;

    private String fieldOfStudy;

    private String location;

    private Instant startDate;

    private Instant endDate;

    private boolean isCurrent;

    private String description;

    private Double gpa;

    private String honors;

    private Instant createdAt;

    private Instant updatedAt;
}

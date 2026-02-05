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
public class Experience {

    private UUID id;

    @JsonIgnore
    private UUID userId;

    private String companyName;

    private String jobTitle;

    private String location;

    private boolean isRemote;

    private Instant startDate;

    private Instant endDate;

    private boolean isCurrentPosition;

    private String description;

    private String employmentType;

    private Instant createdAt;

    private Instant updatedAt;
}

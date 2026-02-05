package com.example.Authentication_System.Domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExperienceRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    @NotBlank(message = "Job title is required")
    @Size(max = 200, message = "Job title must not exceed 200 characters")
    private String jobTitle;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    private boolean isRemote;

    private Instant startDate;

    private Instant endDate;

    private boolean isCurrentPosition;

    private String description;

    @Size(max = 50, message = "Employment type must not exceed 50 characters")
    private String employmentType;
}

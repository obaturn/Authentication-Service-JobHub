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
public class EducationRequest {

    @NotBlank(message = "Institution name is required")
    @Size(max = 200, message = "Institution name must not exceed 200 characters")
    private String institutionName;

    @NotBlank(message = "Degree is required")
    @Size(max = 100, message = "Degree must not exceed 100 characters")
    private String degree;

    @Size(max = 200, message = "Field of study must not exceed 200 characters")
    private String fieldOfStudy;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    private Instant startDate;

    private Instant endDate;

    private boolean isCurrent;

    private String description;

    private Double gpa;

    @Size(max = 200, message = "Honors must not exceed 200 characters")
    private String honors;
}

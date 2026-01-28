package com.example.Authentication_System.Domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecordRequest {

    @NotBlank(message = "Activity type is required")
    private String activityType;

    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    private String metadata;
}
package com.example.Authentication_System.Domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkillRequest {

    @NotBlank(message = "Skill name is required")
    @Size(max = 100, message = "Skill name must not exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Size(max = 30, message = "Proficiency level must not exceed 30 characters")
    private String proficiencyLevel;

    private Integer yearsOfExperience;
}

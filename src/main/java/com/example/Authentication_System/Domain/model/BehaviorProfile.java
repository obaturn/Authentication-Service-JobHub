package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BehaviorProfile {
    private Long id;
    private Integer viewedJobs;
    private Integer appliedJobs;
    private Integer savedJobs;
    
    // Employer specific
    private Integer postedJobs;
    private Integer shortlistedCandidates;
    
    // General
    private Integer timeSpentMinutes;
    private List<String> lastActiveCategories;
    private Instant lastActiveAt;
    
    // Calculated field (Business Logic)
    public String getEngagementLevel() {
        int score = (viewedJobs * 1) + (appliedJobs * 5) + (postedJobs * 10);
        if (score > 50) return "high";
        if (score > 10) return "medium";
        return "low";
    }
}
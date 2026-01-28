package com.example.Authentication_System.Infrastructure.Persistence.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "user_behaviors")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BehaviorProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @Builder.Default
    private Integer viewedJobs = 0;
    
    @Builder.Default
    private Integer appliedJobs = 0;
    
    @Builder.Default
    private Integer savedJobs = 0;
    
    // Employer specific
    @Builder.Default
    private Integer postedJobs = 0;
    
    @Builder.Default
    private Integer shortlistedCandidates = 0;
    
    // General
    @Builder.Default
    private Integer timeSpentMinutes = 0;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_active_categories", joinColumns = @JoinColumn(name = "behavior_id"))
    @Column(name = "category")
    private List<String> lastActiveCategories;
    
    private Instant lastActiveAt;
}
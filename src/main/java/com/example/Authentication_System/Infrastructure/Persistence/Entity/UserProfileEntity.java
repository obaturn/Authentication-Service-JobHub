package com.example.Authentication_System.Infrastructure.Persistence.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(length = 500)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "portfolio_url", length = 500)
    private String portfolioUrl;

    @Column
    private String phone;

    @Column(name = "social_links", columnDefinition = "TEXT")
    private String socialLinks;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "current_salary_range", length = 50)
    private String currentSalaryRange;

    @Column(name = "desired_salary_range", length = 50)
    private String desiredSalaryRange;

    @Column(length = 50)
    private String availability;

    @Column(name = "remote_preference", length = 50)
    private String remotePreference;

    @Column(name = "willing_to_relocate")
    private boolean willingToRelocate;

    @Column(name = "profile_visibility", length = 20)
    private String profileVisibility;

    @Column(name = "profile_completeness")
    private int profileCompleteness;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

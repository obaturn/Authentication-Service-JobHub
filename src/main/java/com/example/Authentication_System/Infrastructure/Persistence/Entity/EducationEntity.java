package com.example.Authentication_System.Infrastructure.Persistence.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "educations")
public class EducationEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "institution_name", nullable = false, length = 200)
    private String institutionName;

    @Column(nullable = false, length = 100)
    private String degree;

    @Column(name = "field_of_study", length = 200)
    private String fieldOfStudy;

    @Column(length = 200)
    private String location;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "is_current")
    private boolean isCurrent;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double gpa;

    @Column(length = 200)
    private String honors;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

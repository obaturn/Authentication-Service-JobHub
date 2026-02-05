package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.Education;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EducationRepository {
    Education save(Education education);
    Optional<Education> findById(UUID id);
    List<Education> findByUserId(UUID userId);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}

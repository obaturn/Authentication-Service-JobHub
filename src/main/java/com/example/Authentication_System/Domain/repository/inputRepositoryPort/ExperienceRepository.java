package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.Experience;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceRepository {
    Experience save(Experience experience);
    Optional<Experience> findById(UUID id);
    List<Experience> findByUserId(UUID userId);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}

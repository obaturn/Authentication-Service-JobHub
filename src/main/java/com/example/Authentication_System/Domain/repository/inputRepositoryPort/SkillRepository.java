package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.Skill;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository {
    Skill save(Skill skill);
    Optional<Skill> findById(UUID id);
    List<Skill> findByUserId(UUID userId);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}

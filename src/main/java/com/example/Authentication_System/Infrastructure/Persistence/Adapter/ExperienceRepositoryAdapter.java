package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.Experience;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.ExperienceRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.ExperienceEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.ExperienceJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.ExperienceMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Component
public class ExperienceRepositoryAdapter implements ExperienceRepository {

    private final ExperienceJpaRepository experienceJpaRepository;
    private final ExperienceMapper experienceMapper;

    public ExperienceRepositoryAdapter(ExperienceJpaRepository experienceJpaRepository, ExperienceMapper experienceMapper) {
        this.experienceJpaRepository = experienceJpaRepository;
        this.experienceMapper = experienceMapper;
    }

    @Override
    public Experience save(Experience experience) {
        ExperienceEntity experienceEntity = experienceMapper.toEntity(experience);
        ExperienceEntity savedEntity = experienceJpaRepository.save(experienceEntity);
        return experienceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Experience> findById(UUID id) {
        return experienceJpaRepository.findById(id)
                .map(experienceMapper::toDomain);
    }

    @Override
    public List<Experience> findByUserId(UUID userId) {
        return experienceJpaRepository.findByUserId(userId).stream()
                .map(experienceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        experienceJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return experienceJpaRepository.existsById(id);
    }
}

package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.Education;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.EducationRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.EducationEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.EducationJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.EducationMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Component
public class EducationRepositoryAdapter implements EducationRepository {

    private final EducationJpaRepository educationJpaRepository;
    private final EducationMapper educationMapper;

    public EducationRepositoryAdapter(EducationJpaRepository educationJpaRepository, EducationMapper educationMapper) {
        this.educationJpaRepository = educationJpaRepository;
        this.educationMapper = educationMapper;
    }

    @Override
    public Education save(Education education) {
        EducationEntity educationEntity = educationMapper.toEntity(education);
        EducationEntity savedEntity = educationJpaRepository.save(educationEntity);
        return educationMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Education> findById(UUID id) {
        return educationJpaRepository.findById(id)
                .map(educationMapper::toDomain);
    }

    @Override
    public List<Education> findByUserId(UUID userId) {
        return educationJpaRepository.findByUserId(userId).stream()
                .map(educationMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        educationJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return educationJpaRepository.existsById(id);
    }
}

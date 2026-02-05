package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.Skill;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.SkillRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.SkillEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.SkillJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.SkillMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Component
public class SkillRepositoryAdapter implements SkillRepository {

    private final SkillJpaRepository skillJpaRepository;
    private final SkillMapper skillMapper;

    public SkillRepositoryAdapter(SkillJpaRepository skillJpaRepository, SkillMapper skillMapper) {
        this.skillJpaRepository = skillJpaRepository;
        this.skillMapper = skillMapper;
    }

    @Override
    public Skill save(Skill skill) {
        SkillEntity skillEntity = skillMapper.toEntity(skill);
        SkillEntity savedEntity = skillJpaRepository.save(skillEntity);
        return skillMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Skill> findById(UUID id) {
        return skillJpaRepository.findById(id)
                .map(skillMapper::toDomain);
    }

    @Override
    public List<Skill> findByUserId(UUID userId) {
        return skillJpaRepository.findByUserId(userId).stream()
                .map(skillMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        skillJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return skillJpaRepository.existsById(id);
    }
}

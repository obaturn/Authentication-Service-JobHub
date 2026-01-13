package com.example.Authentication_System.Infrastructure.Persistence.Adapter;

import com.example.Authentication_System.Domain.model.AuditLog;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.AuditLogRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.AuditLogEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.AuditLogDataJpaRepository;
import com.example.Authentication_System.Infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogDataJpaRepository jpa;

    public AuditLogRepositoryAdapter(AuditLogDataJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = UserMapper.toEntity(auditLog);
        AuditLogEntity saved = jpa.save(entity);
        return UserMapper.toDomain(saved);
    }
}
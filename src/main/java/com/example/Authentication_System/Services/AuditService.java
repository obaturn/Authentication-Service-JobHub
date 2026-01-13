package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.AuditLog;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logEvent(UUID userId, String action, String resource, UUID resourceId, String details, String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
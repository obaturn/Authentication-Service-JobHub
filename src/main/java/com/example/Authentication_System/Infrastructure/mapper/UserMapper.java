package com.example.Authentication_System.Infrastructure.mapper;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.model.Role;
import com.example.Authentication_System.Domain.model.Session;
import com.example.Authentication_System.Domain.model.Permission;
import com.example.Authentication_System.Domain.model.AuditLog;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UserMapper {

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .mfaEnabled(user.isMfaEnabled())
                .mfaSecret(user.getMfaSecret())
                .googleId(user.getGoogleId())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .location(user.getLocation())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(user.getCreatedAt(), ZoneId.systemDefault()) : null)
                .updatedAt(user.getUpdatedAt() != null ?
                        LocalDateTime.ofInstant(user.getUpdatedAt(), ZoneId.systemDefault()) : null)
                .lastLoginAt(user.getLastLoginAt() != null ?
                        LocalDateTime.ofInstant(user.getLastLoginAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    public static User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .userType(entity.getUserType())
                .status(entity.getStatus())
                .emailVerified(entity.isEmailVerified())
                .mfaEnabled(entity.isMfaEnabled())
                .mfaSecret(entity.getMfaSecret())
                .googleId(entity.getGoogleId())
                .avatarUrl(entity.getAvatarUrl())
                .phone(entity.getPhone())
                .location(entity.getLocation())
                .bio(entity.getBio())
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(entity.getUpdatedAt() != null ?
                        entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .lastLoginAt(entity.getLastLoginAt() != null ?
                        entity.getLastLoginAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    public static RoleEntity toEntity(Role role) {
        return RoleEntity.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(role.getCreatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    public static Role toDomain(RoleEntity entity) {
        return Role.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    public static SessionEntity toEntity(Session session) {
        return SessionEntity.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .tokenHash(session.getTokenHash())
                .ipAddress(session.getIpAddress() != null ? session.getIpAddress().getHostAddress() : null)
                .userAgent(session.getUserAgent())
                .expiresAt(session.getExpiresAt() != null ?
                        LocalDateTime.ofInstant(session.getExpiresAt(), ZoneId.systemDefault()) : null)
                .createdAt(session.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(session.getCreatedAt(), ZoneId.systemDefault()) : null)
                .revokedAt(session.getRevokedAt() != null ?
                        LocalDateTime.ofInstant(session.getRevokedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    public static Session toDomain(SessionEntity entity) {
        InetAddress ipAddress = null;
        if (entity.getIpAddress() != null) {
            try {
                ipAddress = InetAddress.getByName(entity.getIpAddress());
            } catch (UnknownHostException e) {
                // Handle exception if needed
            }
        }
        return Session.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .ipAddress(ipAddress)
                .userAgent(entity.getUserAgent())
                .expiresAt(entity.getExpiresAt() != null ?
                        entity.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .revokedAt(entity.getRevokedAt() != null ?
                        entity.getRevokedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    public static PermissionEntity toEntity(Permission permission) {
        return PermissionEntity.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .resource(permission.getResource())
                .action(permission.getAction())
                .createdAt(permission.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(permission.getCreatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    public static Permission toDomain(PermissionEntity entity) {
        return Permission.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .resource(entity.getResource())
                .action(entity.getAction())
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    public static AuditLogEntity toEntity(AuditLog auditLog) {
        return AuditLogEntity.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .action(auditLog.getAction())
                .resource(auditLog.getResource())
                .resourceId(auditLog.getResourceId())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(auditLog.getCreatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    public static AuditLog toDomain(AuditLogEntity entity) {
        return AuditLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .action(entity.getAction())
                .resource(entity.getResource())
                .resourceId(entity.getResourceId())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }
}

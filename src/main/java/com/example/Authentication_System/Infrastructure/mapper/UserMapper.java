package com.example.Authentication_System.Infrastructure.mapper;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.model.Role;
import com.example.Authentication_System.Domain.model.UserRole;
import com.example.Authentication_System.Domain.model.Session;
import com.example.Authentication_System.Domain.model.Permission;
import com.example.Authentication_System.Domain.model.AuditLog;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.*;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class UserMapper {

    private final UserProfileMapper userProfileMapper;

    public UserMapper(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    public UserEntity toEntity(User user) {
        UserEntity.UserEntityBuilder builder = UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .emailVerificationToken(user.getEmailVerificationToken())
                .emailVerificationExpiresAt(user.getEmailVerificationExpiresAt() != null ?
                        LocalDateTime.ofInstant(user.getEmailVerificationExpiresAt(), ZoneId.systemDefault()) : null)
                .passwordResetToken(user.getPasswordResetToken())
                .passwordResetExpiresAt(user.getPasswordResetExpiresAt() != null ?
                        LocalDateTime.ofInstant(user.getPasswordResetExpiresAt(), ZoneId.systemDefault()) : null)
                .mfaEnabled(user.isMfaEnabled())
                .mfaSecret(user.getMfaSecret())
                .googleId(user.getGoogleId())
                .createdAt(user.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(user.getCreatedAt(), ZoneId.systemDefault()) : null)
                .updatedAt(user.getUpdatedAt() != null ?
                        LocalDateTime.ofInstant(user.getUpdatedAt(), ZoneId.systemDefault()) : null)
                .lastLoginAt(user.getLastLoginAt() != null ?
                        LocalDateTime.ofInstant(user.getLastLoginAt(), ZoneId.systemDefault()) : null);

        UserEntity userEntity = builder.build();

        if (user.getUserProfile() != null) {
            UserProfileEntity userProfileEntity = userProfileMapper.toEntity(user.getUserProfile());
            userProfileEntity.setUser(userEntity); // Set the bidirectional relationship
            userEntity.setUserProfile(userProfileEntity);
        }

        return userEntity;
    }

    public User toDomain(UserEntity entity) {
        User.UserBuilder builder = User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .userType(entity.getUserType())
                .status(entity.getStatus())
                .emailVerified(entity.isEmailVerified())
                .emailVerificationToken(entity.getEmailVerificationToken())
                .emailVerificationExpiresAt(entity.getEmailVerificationExpiresAt() != null ?
                        entity.getEmailVerificationExpiresAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .passwordResetToken(entity.getPasswordResetToken())
                .passwordResetExpiresAt(entity.getPasswordResetExpiresAt() != null ?
                        entity.getPasswordResetExpiresAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .mfaEnabled(entity.isMfaEnabled())
                .mfaSecret(entity.getMfaSecret())
                .googleId(entity.getGoogleId())
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(entity.getUpdatedAt() != null ?
                        entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .lastLoginAt(entity.getLastLoginAt() != null ?
                        entity.getLastLoginAt().atZone(ZoneId.systemDefault()).toInstant() : null);

        if (entity.getUserProfile() != null) {
            builder.userProfile(userProfileMapper.toDomain(entity.getUserProfile()));
        }

        return builder.build();
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

    public static UserRoleEntity toEntity(UserRole userRole) {
        return UserRoleEntity.builder()
                .id(userRole.getId())
                .userId(userRole.getUserId())
                .roleId(userRole.getRoleId())
                .assignedAt(userRole.getAssignedAt() != null ?
                        LocalDateTime.ofInstant(userRole.getAssignedAt(), ZoneId.systemDefault()) : null)
                .assignedBy(userRole.getAssignedBy())
                .build();
    }

    public static UserRole toDomain(UserRoleEntity entity) {
        return UserRole.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .roleId(entity.getRoleId())
                .assignedAt(entity.getAssignedAt() != null ?
                        entity.getAssignedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .assignedBy(entity.getAssignedBy())
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

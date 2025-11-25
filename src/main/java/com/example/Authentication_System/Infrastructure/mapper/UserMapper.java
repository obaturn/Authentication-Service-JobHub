package com.example.Authentication_System.Infrastructure.mapper;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class UserMapper {

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(user.getCreatedAt(), ZoneId.systemDefault()) : null)
                .updatedAt(user.getUpdatedAt() != null ?
                        LocalDateTime.ofInstant(user.getUpdatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    public static User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt() != null ?
                        entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(entity.getUpdatedAt() != null ?
                        entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .build();
    }
}

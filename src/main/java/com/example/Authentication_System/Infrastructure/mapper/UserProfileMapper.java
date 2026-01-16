package com.example.Authentication_System.Infrastructure.mapper;

import com.example.Authentication_System.Domain.model.UserProfile;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserProfileEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    private final ModelMapper modelMapper;

    public UserProfileMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserProfile toDomain(UserProfileEntity entity) {
        return modelMapper.map(entity, UserProfile.class);
    }

    public UserProfileEntity toEntity(UserProfile domain) {
        return modelMapper.map(domain, UserProfileEntity.class);
    }
}

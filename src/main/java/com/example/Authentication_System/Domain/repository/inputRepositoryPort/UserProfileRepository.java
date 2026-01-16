package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.UserProfile;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {
    UserProfile save(UserProfile userProfile);
    Optional<UserProfile> findByUserId(UUID userId);
}

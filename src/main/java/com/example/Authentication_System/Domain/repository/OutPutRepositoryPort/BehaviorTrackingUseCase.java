package com.example.Authentication_System.Domain.repository.OutPutRepositoryPort;

import com.example.Authentication_System.Domain.model.BehaviorProfile;

import java.util.UUID;

public interface BehaviorTrackingUseCase {
    void trackJobView(UUID userId, String jobId);
    void trackJobApply(UUID userId, String jobId);
    void trackJobSave(UUID userId, String jobId);
    void trackJobPost(UUID userId, String jobId);
    void trackTimeSpent(UUID userId, int minutes);
    BehaviorProfile getBehaviorProfile(UUID userId);
}
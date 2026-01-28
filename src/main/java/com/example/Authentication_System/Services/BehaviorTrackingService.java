package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.model.BehaviorProfile;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.BehaviorTrackingUseCase;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.BehaviorProfileEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.BehaviorProfileDataJpaRepository;
import com.example.Authentication_System.Infrastructure.Persistence.Repository.UserDataJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class BehaviorTrackingService implements BehaviorTrackingUseCase {

    private final BehaviorProfileDataJpaRepository behaviorRepository;
    private final UserDataJpaRepository userRepository;

    public BehaviorTrackingService(BehaviorProfileDataJpaRepository behaviorRepository, UserDataJpaRepository userRepository) {
        this.behaviorRepository = behaviorRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void trackJobView(UUID userId, String jobId) {
        BehaviorProfileEntity profile = getOrCreateProfile(userId);
        profile.setViewedJobs(profile.getViewedJobs() + 1);
        profile.setLastActiveAt(Instant.now());
        behaviorRepository.save(profile);
    }

    @Override
    @Transactional
    public void trackJobApply(UUID userId, String jobId) {
        BehaviorProfileEntity profile = getOrCreateProfile(userId);
        profile.setAppliedJobs(profile.getAppliedJobs() + 1);
        profile.setLastActiveAt(Instant.now());
        behaviorRepository.save(profile);
    }

    @Override
    @Transactional
    public void trackJobSave(UUID userId, String jobId) {
        BehaviorProfileEntity profile = getOrCreateProfile(userId);
        profile.setSavedJobs(profile.getSavedJobs() + 1);
        profile.setLastActiveAt(Instant.now());
        behaviorRepository.save(profile);
    }

    @Override
    @Transactional
    public void trackJobPost(UUID userId, String jobId) {
        BehaviorProfileEntity profile = getOrCreateProfile(userId);
        profile.setPostedJobs(profile.getPostedJobs() + 1);
        profile.setLastActiveAt(Instant.now());
        behaviorRepository.save(profile);
    }

    @Override
    @Transactional
    public void trackTimeSpent(UUID userId, int minutes) {
        BehaviorProfileEntity profile = getOrCreateProfile(userId);
        profile.setTimeSpentMinutes(profile.getTimeSpentMinutes() + minutes);
        profile.setLastActiveAt(Instant.now());
        behaviorRepository.save(profile);
    }

    @Override
    public BehaviorProfile getBehaviorProfile(UUID userId) {
        BehaviorProfileEntity entity = getOrCreateProfile(userId);
        return BehaviorProfile.builder()
                .id(entity.getId())
                .viewedJobs(entity.getViewedJobs())
                .appliedJobs(entity.getAppliedJobs())
                .savedJobs(entity.getSavedJobs())
                .postedJobs(entity.getPostedJobs())
                .shortlistedCandidates(entity.getShortlistedCandidates())
                .timeSpentMinutes(entity.getTimeSpentMinutes())
                .lastActiveCategories(entity.getLastActiveCategories())
                .lastActiveAt(entity.getLastActiveAt())
                .build();
    }

    private BehaviorProfileEntity getOrCreateProfile(UUID userId) {
        return behaviorRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    
                    BehaviorProfileEntity newProfile = BehaviorProfileEntity.builder()
                            .user(user)
                            .viewedJobs(0)
                            .appliedJobs(0)
                            .savedJobs(0)
                            .postedJobs(0)
                            .shortlistedCandidates(0)
                            .timeSpentMinutes(0)
                            .lastActiveCategories(new ArrayList<>())
                            .lastActiveAt(Instant.now())
                            .build();
                    
                    return behaviorRepository.save(newProfile);
                });
    }
}
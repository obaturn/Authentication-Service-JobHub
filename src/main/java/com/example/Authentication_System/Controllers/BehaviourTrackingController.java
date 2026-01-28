package com.example.Authentication_System.Controllers;

import com.example.Authentication_System.Domain.model.BehaviorProfile;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.BehaviorTrackingUseCase;
import com.example.Authentication_System.Security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/behavior")
public class BehaviourTrackingController {
    private final BehaviorTrackingUseCase behaviorService;
    private final JwtUtils jwtUtils;

    public BehaviourTrackingController(BehaviorTrackingUseCase behaviorService, JwtUtils jwtUtils) {
        this.behaviorService = behaviorService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/track")
    public ResponseEntity<Void> trackEvent(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> event) {
        String jwt = token.substring(7);
        String userIdStr = jwtUtils.getUserIdFromToken(jwt);
        UUID userId = UUID.fromString(userIdStr);

        String eventType = (String) event.get("event");
        String jobId = (String) event.get("jobId");
        Integer duration = (Integer) event.get("duration");

        if (eventType == null) {
            throw new IllegalArgumentException("Event type is required");
        }

        switch (eventType) {
            case "job_viewed":
                behaviorService.trackJobView(userId, jobId);
                break;
            case "job_applied":
                behaviorService.trackJobApply(userId, jobId);
                break;
            case "job_saved":
                behaviorService.trackJobSave(userId, jobId);
                break;
            case "job_posted":
                behaviorService.trackJobPost(userId, jobId);
                break;
            case "time_spent":
                if (duration != null) {
                    behaviorService.trackTimeSpent(userId, duration);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<BehaviorProfile> getProfile(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        String userIdStr = jwtUtils.getUserIdFromToken(jwt);
        UUID userId = UUID.fromString(userIdStr);

        BehaviorProfile profile = behaviorService.getBehaviorProfile(userId);
        return ResponseEntity.ok(profile);
    }
}
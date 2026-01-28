package com.example.Authentication_System.Services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, this::newBucket);
    }

    private Bucket newBucket(String key) {
        // Define your rate limiting rules here
        // Example: 10 requests per minute
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    // You can define different types of buckets for different scenarios
    public Bucket resolveLoginBucket(String ipAddress) {
        // Example: 5 login attempts per minute per IP
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return cache.computeIfAbsent("login:" + ipAddress, k -> Bucket4j.builder().addLimit(limit).build());
    }

    public Bucket resolveRegistrationBucket(String ipAddress) {
        // Example: 100 registration attempts per minute per IP for easier testing
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return cache.computeIfAbsent("register:" + ipAddress, k -> Bucket4j.builder().addLimit(limit).build());
    }

    // User-based rate limiting for enhanced security
    public Bucket resolveUserLoginBucket(String userId) {
        // Stricter limits per user account: 3 attempts per 5 minutes
        Bandwidth limit = Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(5)));
        return cache.computeIfAbsent("user-login:" + userId, k -> Bucket4j.builder().addLimit(limit).build());
    }

    public Bucket resolveUserRegistrationBucket(String email) {
        // 1 registration attempt per email per 10 minutes
        Bandwidth limit = Bandwidth.classic(1, Refill.greedy(1, Duration.ofMinutes(10)));
        return cache.computeIfAbsent("user-register:" + email.toLowerCase(), k -> Bucket4j.builder().addLimit(limit).build());
    }

    public Bucket resolvePasswordResetBucket(String email) {
        // 2 password reset attempts per email per hour
        Bandwidth limit = Bandwidth.classic(2, Refill.greedy(2, Duration.ofHours(1)));
        return cache.computeIfAbsent("password-reset:" + email.toLowerCase(), k -> Bucket4j.builder().addLimit(limit).build());
    }
}

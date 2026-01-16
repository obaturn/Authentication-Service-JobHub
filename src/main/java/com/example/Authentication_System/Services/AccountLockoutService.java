package com.example.Authentication_System.Services;

import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private final UserRepository userRepository;

    @Value("${security.account-lockout.attempts-threshold:5}")
    private int maxFailedAttempts;

    @Value("${security.account-lockout.lock-duration-minutes:15}")
    private long lockDurationMinutes;

    @Value("${security.account-lockout.progressive-delays:true}")
    private boolean progressiveDelaysEnabled;

    /**
     * Records a failed login attempt and applies lockout if threshold exceeded
     */
    @Transactional
    public void recordFailedAttempt(UUID userId, String ipAddress, String userAgent) {
        var userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("Attempted to record failed login for non-existent user: {}", userId);
            return;
        }

        var user = userOptional.get();
        int currentAttempts = user.getFailedLoginAttempts() + 1;
        Instant now = Instant.now();

        user.setFailedLoginAttempts(currentAttempts);
        user.setLastFailedAttemptAt(now);

        // Apply lockout if threshold exceeded
        if (currentAttempts >= maxFailedAttempts) {
            Instant lockUntil = calculateLockoutDuration(currentAttempts);
            user.setAccountLockedUntil(lockUntil);

            log.warn("Account locked for user {} due to {} failed attempts. Locked until: {}",
                    userId, currentAttempts, lockUntil);
        }

        userRepository.save(user);
    }

    /**
     * Records a successful login and resets failed attempt counter
     */
    @Transactional
    public void recordSuccessfulLogin(UUID userId) {
        var userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            var user = userOptional.get();
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setLastFailedAttemptAt(null);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            log.info("Successful login recorded for user: {}", userId);
        }
    }

    /**
     * Checks if account is currently locked
     */
    public boolean isAccountLocked(UUID userId) {
        var userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return false;
        }

        Instant lockedUntil = userOptional.get().getAccountLockedUntil();
        if (lockedUntil == null) {
            return false;
        }

        boolean isLocked = Instant.now().isBefore(lockedUntil);
        if (!isLocked && userOptional.get().getFailedLoginAttempts() > 0) {
            // Lock has expired, reset counter
            resetFailedAttempts(userId);
        }

        return isLocked;
    }

    /**
     * Gets remaining lockout time in seconds
     */
    public long getRemainingLockoutTime(UUID userId) {
        var userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return 0;
        }

        Instant lockedUntil = userOptional.get().getAccountLockedUntil();
        if (lockedUntil == null) {
            return 0;
        }

        Duration remaining = Duration.between(Instant.now(), lockedUntil);
        return Math.max(0, remaining.getSeconds());
    }

    /**
     * Calculates progressive delay for rapid failed attempts
     */
    public long getProgressiveDelay(int failedAttempts) {
        if (!progressiveDelaysEnabled || failedAttempts < 3) {
            return 0;
        }

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s...
        return (long) Math.pow(2, failedAttempts - 3);
    }

    /**
     * Manually unlock account (admin function)
     */
    @Transactional
    public void unlockAccount(UUID userId) {
        var userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            var user = userOptional.get();
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            user.setLastFailedAttemptAt(null);
            userRepository.save(user);

            log.info("Account manually unlocked for user: {}", userId);
        }
    }

    private Instant calculateLockoutDuration(int failedAttempts) {
        // Progressive lockout: more attempts = longer lock
        long minutes = lockDurationMinutes;
        if (failedAttempts >= 10) {
            minutes = 60; // 1 hour
        } else if (failedAttempts >= 7) {
            minutes = 30; // 30 minutes
        }
        // Default lockDurationMinutes for 5-6 attempts

        return Instant.now().plusSeconds(minutes * 60);
    }

    @Transactional
    private void resetFailedAttempts(UUID userId) {
        var userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            var user = userOptional.get();
            user.setFailedLoginAttempts(0);
            user.setLastFailedAttemptAt(null);
            userRepository.save(user);
        }
    }
}
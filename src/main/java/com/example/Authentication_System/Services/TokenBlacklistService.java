package com.example.Authentication_System.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * Add a token to the blacklist with TTL matching token expiry
     */
    public void blacklistToken(String token, long expiryTimeMillis) {
        try {
            String key = BLACKLIST_PREFIX + token;
            long ttlSeconds = TimeUnit.MILLISECONDS.toSeconds(expiryTimeMillis - System.currentTimeMillis());

            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
                log.debug("Token blacklisted with TTL: {} seconds", ttlSeconds);
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
            // Continue operation even if Redis fails - security degradation but not failure
        }
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            String value = redisTemplate.opsForValue().get(key);
            return "blacklisted".equals(value);
        } catch (Exception e) {
            log.error("Failed to check token blacklist", e);
            // Allow token if Redis is unavailable - fail open for availability
            return false;
        }
    }

    /**
     * Remove a token from blacklist (useful for testing or manual intervention)
     */
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            log.info("Token removed from blacklist: {}", token.substring(0, 10) + "...");
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist", e);
        }
    }

    /**
     * Get blacklist statistics
     */
    public long getBlacklistSize() {
        try {
            return redisTemplate.keys(BLACKLIST_PREFIX + "*").size();
        } catch (Exception e) {
            log.error("Failed to get blacklist size", e);
            return 0;
        }
    }

    /**
     * Clear all blacklisted tokens (admin function)
     */
    public void clearBlacklist() {
        try {
            redisTemplate.delete(redisTemplate.keys(BLACKLIST_PREFIX + "*"));
            log.warn("Blacklist cleared by admin");
        } catch (Exception e) {
            log.error("Failed to clear blacklist", e);
        }
    }
}
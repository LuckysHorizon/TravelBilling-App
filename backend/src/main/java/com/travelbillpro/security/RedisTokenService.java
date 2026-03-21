package com.travelbillpro.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token service that uses Redis when available, falls back to an in-memory
 * ConcurrentHashMap when Redis is not configured. The in-memory fallback is
 * NOT suitable for production clusters but works perfectly for single-node
 * development without Docker/Redis.
 */
@Service
@Slf4j
public class RedisTokenService {

    @Nullable
    private final StringRedisTemplate redisTemplate;

    // In-memory fallback store
    private final Map<String, String> memoryStore = new ConcurrentHashMap<>();

    // Prefix for refresh tokens (JTIs)
    private static final String REFRESH_TOKEN_PREFIX = "rt:";
    // Prefix for revoked access tokens (Blacklisting)
    private static final String REVOKED_TOKEN_PREFIX = "bl:";

    public RedisTokenService(@Nullable StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            log.warn("Redis is not available. Using in-memory token store. This is NOT suitable for production.");
        } else {
            log.info("Redis token service initialized successfully.");
        }
    }

    public void storeRefreshTokenJti(String username, String jti, long durationMs) {
        String key = REFRESH_TOKEN_PREFIX + username;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, jti, Duration.ofMillis(durationMs));
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for store, falling back to memory: {}", e.getMessage());
            }
        }
        memoryStore.put(key, jti);
    }

    public boolean validateRefreshTokenJti(String username, String jti) {
        String key = REFRESH_TOKEN_PREFIX + username;
        if (redisTemplate != null) {
            try {
                String storedJti = redisTemplate.opsForValue().get(key);
                return jti.equals(storedJti);
            } catch (Exception e) {
                log.warn("Redis unavailable for validate, falling back to memory: {}", e.getMessage());
            }
        }
        return jti.equals(memoryStore.get(key));
    }

    public void deleteRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for delete, falling back to memory: {}", e.getMessage());
            }
        }
        memoryStore.remove(key);
    }

    public void blacklistAccessToken(String jti, long remainingTimeMs) {
        String key = REVOKED_TOKEN_PREFIX + jti;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(remainingTimeMs));
                return;
            } catch (Exception e) {
                log.warn("Redis unavailable for blacklist, falling back to memory: {}", e.getMessage());
            }
        }
        memoryStore.put(key, "revoked");
    }

    public boolean isTokenBlacklisted(String jti) {
        String key = REVOKED_TOKEN_PREFIX + jti;
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            } catch (Exception e) {
                log.warn("Redis unavailable for blacklist check, falling back to memory: {}", e.getMessage());
            }
        }
        return memoryStore.containsKey(key);
    }
}

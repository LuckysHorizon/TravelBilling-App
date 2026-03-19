package com.travelbillpro.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final StringRedisTemplate redisTemplate;
    
    // Prefix for refresh tokens (JTIs)
    private static final String REFRESH_TOKEN_PREFIX = "rt:";
    // Prefix for revoked access tokens (Blacklisting)
    private static final String REVOKED_TOKEN_PREFIX = "bl:";

    public void storeRefreshTokenJti(String username, String jti, long durationMs) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + username,
                jti,
                Duration.ofMillis(durationMs)
        );
    }

    public boolean validateRefreshTokenJti(String username, String jti) {
        String storedJti = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
        return jti.equals(storedJti);
    }

    public void deleteRefreshToken(String username) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);
    }

    public void blacklistAccessToken(String jti, long remainingTimeMs) {
        redisTemplate.opsForValue().set(
                REVOKED_TOKEN_PREFIX + jti,
                "revoked",
                Duration.ofMillis(remainingTimeMs)
        );
    }

    public boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_TOKEN_PREFIX + jti));
    }
}

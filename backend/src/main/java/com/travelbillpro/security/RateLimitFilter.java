package com.travelbillpro.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed sliding window rate limiter.
 *
 * Three tiers:
 *   1. IP-based (all requests):  app.rate-limit.ip   requests/minute
 *   2. User-based (authenticated): app.rate-limit.user requests/minute
 *   3. Login endpoint:  10/minute per IP (brute force protection)
 *
 * Returns HTTP 429 with Retry-After header when exceeded.
 * Skips health endpoints to avoid polluting counters.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.ip:500}")
    private int ipLimit;

    @Value("${app.rate-limit.user:200}")
    private int userLimit;

    private static final int LOGIN_LIMIT = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Tier 1: IP-based rate limiting
        String ipKey = "rate:ip:" + clientIp;
        if (isRateLimited(ipKey, ipLimit)) {
            log.warn("Rate limit exceeded for IP: {} on {}", clientIp, path);
            sendRateLimitResponse(response);
            return;
        }

        // Tier 2: Stricter limit on login endpoint
        if ("/api/auth/login".equals(path)) {
            String loginKey = "rate:login:" + clientIp;
            if (isRateLimited(loginKey, LOGIN_LIMIT)) {
                log.warn("Login rate limit exceeded for IP: {}", clientIp);
                sendRateLimitResponse(response);
                return;
            }
        }

        // Tier 3: User-based rate limiting (if authenticated)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String userKey = "rate:user:" + auth.getName();
            if (isRateLimited(userKey, userLimit)) {
                log.warn("Rate limit exceeded for user: {} on {}", auth.getName(), path);
                sendRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key, int limit) {
        if (redisTemplate == null) return false; // No Redis → fail open
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
            return count != null && count > limit;
        } catch (Exception e) {
            // Redis down — fail open (allow request)
            log.warn("Rate limit check failed (Redis error): {}", e.getMessage());
            return false;
        }
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(response.getWriter(),
            Map.of("error", "Too many requests", "retryAfterSeconds", 60));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.travelbillpro.controller;

import com.travelbillpro.dto.AuthDto;
import com.travelbillpro.entity.Organization;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.Role;
import com.travelbillpro.repository.UserRepository;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.security.JwtService;
import com.travelbillpro.security.RedisTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final UserRepository userRepository;

    @Value("${app.security.jwt.access-token-expiration-ms}")
    private long accessTokenExpiry;

    @Value("${app.security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpiry;

    @Value("${spring.datasource.url}")
    private String masterDatasourceUrl;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDto.LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Reset failed attempts on success
            if (user.getFailedAttempts() > 0) {
                user.setFailedAttempts(0);
                userRepository.save(user);
            }

            setTokenCookies(response, userDetails, user);

            // Build response with org context
            Organization org = user.getOrganization();

            AuthDto.AuthResponse authResponse = AuthDto.AuthResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .orgId(org != null ? org.getId() : null)
                    .orgName(org != null ? org.getName() : null)
                    .orgSlug(org != null ? org.getSlug() : null)
                    .build();

            return ResponseEntity.ok(authResponse);

        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getUsername());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        } catch (LockedException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is locked"));
        }
    }

    private void handleFailedLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            userRepository.save(user);
        });
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token missing"));
        }

        try {
            String username = jwtService.extractUsername(refreshToken);
            String jti = jwtService.extractClaim(refreshToken, claims -> claims.getId());

            if (username != null && redisTokenService.validateRefreshTokenJti(username, jti)) {
                User user = userRepository.findByUsername(username).orElseThrow();
                CustomUserDetails userDetails = new CustomUserDetails(user);

                if (jwtService.isTokenValid(refreshToken, userDetails)) {
                    setTokenCookies(response, userDetails, user);
                    return ResponseEntity.ok().build();
                }
            }
        } catch (Exception e) {
            // Token invalid
        }

        clearTokenCookies(response);
        return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken != null) {
            try {
                String username = jwtService.extractUsername(refreshToken);
                redisTokenService.deleteRefreshToken(username);
            } catch (Exception e) {
                // Ignore missing/invalid token on logout
            }
        }

        clearTokenCookies(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        Organization org = user.getOrganization();

        AuthDto.AuthResponse authResponse = AuthDto.AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .orgId(org != null ? org.getId() : null)
                .orgName(org != null ? org.getName() : null)
                .orgSlug(org != null ? org.getSlug() : null)
                .build();

        return ResponseEntity.ok(authResponse);
    }

    private void setTokenCookies(HttpServletResponse response, CustomUserDetails userDetails, User user) {
        // Get org context for JWT claims
        Organization org = user.getOrganization();
        Long orgId = org != null ? org.getId() : null;
        String orgSlug = org != null ? org.getSlug() : null;
        String dbUrl = resolveJwtDbUrl(user, org);

        // Generate Access Token with org context
        String accessToken = jwtService.generateToken(userDetails, orgId, orgSlug, dbUrl);

        // Generate Refresh Token with JTI + org context
        String jti = UUID.randomUUID().toString();
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("jti", jti);
        if (orgId != null) extraClaims.put("orgId", orgId);
        if (orgSlug != null) extraClaims.put("orgSlug", orgSlug);
        if (dbUrl != null) extraClaims.put("dbUrl", dbUrl);
        String refreshToken = jwtService.generateToken(extraClaims, userDetails);

        // Store JTI in Redis
        redisTokenService.storeRefreshTokenJti(userDetails.getUsername(), jti, refreshTokenExpiry);

        // Access Token Cookie
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (accessTokenExpiry / 1000));
        response.addCookie(accessCookie);

        // Refresh Token Cookie
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge((int) (refreshTokenExpiry / 1000));
        response.addCookie(refreshCookie);
    }

    private void clearTokenCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String resolveJwtDbUrl(User user, Organization org) {
        if (user.getRole() == Role.SUPER_ADMIN) {
            return null;
        }
        if (org == null) {
            return null;
        }

        String orgDbUrl = org.getDbUrl();
        if (orgDbUrl == null || orgDbUrl.isBlank() || isLocalDbUrl(orgDbUrl)) {
            return masterDatasourceUrl;
        }
        return orgDbUrl;
    }

    private boolean isLocalDbUrl(String dbUrl) {
        String normalized = dbUrl.toLowerCase();
        return normalized.contains("localhost") || normalized.contains("127.0.0.1");
    }
}

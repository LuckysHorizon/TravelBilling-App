package com.travelbillpro.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Map;

/**
 * Startup validation that runs in ALL profiles.
 * <p>
 * In the "prod" profile, missing or insecure configuration causes immediate
 * startup failure — preventing a broken deployment from serving traffic.
 * <p>
 * In the "dev" profile, issues are logged as warnings so developers aren't
 * blocked during local development.
 */
@Configuration
@Slf4j
public class ProductionEnvSanityCheck {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${app.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.cors.allowed-origins:}")
    private String corsOrigins;

    // ── Shared runner (all profiles) ─────────────────────────────────

    @Bean
    public ApplicationRunner envSanityRunner(Environment environment) {
        return args -> {
            String[] activeProfiles = environment.getActiveProfiles();
            String profileDisplay = activeProfiles.length > 0
                    ? String.join(", ", activeProfiles)
                    : "default (no profile set)";

            log.info("╔══════════════════════════════════════════════════╗");
            log.info("║  TravelBill Pro — Environment Startup Check     ║");
            log.info("╠══════════════════════════════════════════════════╣");
            log.info("║  Active Profile(s): {}", profileDisplay);
            log.info("║  Database URL:      {}", maskJdbcUrl(datasourceUrl));
            log.info("║  CORS Origins:      {}", corsOrigins);
            log.info("╚══════════════════════════════════════════════════╝");

            // Warn about quoted JDBC URLs (common Render misconfiguration)
            if (datasourceUrl != null && datasourceUrl.length() >= 2) {
                if ((datasourceUrl.startsWith("\"") && datasourceUrl.endsWith("\""))
                        || (datasourceUrl.startsWith("'") && datasourceUrl.endsWith("'"))) {
                    log.error("SPRING_DATASOURCE_URL appears to be wrapped in quotes. "
                            + "Remove quotes in your environment variable settings.");
                }
            }

            // Warn about dot-notation env keys (should be UPPER_SNAKE_CASE)
            Map<String, String> env = System.getenv();
            if (env.containsKey("spring.datasource.hikari.maximum-pool-size")) {
                log.error("Detected unsupported env key 'spring.datasource.hikari.maximum-pool-size'. "
                        + "Use 'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE' instead.");
            }
        };
    }

    // ── Production-only strict validation ────────────────────────────

    @Bean
    @Profile("prod")
    public ApplicationRunner prodEnvStrictValidator() {
        return args -> {
            log.info("Running PRODUCTION environment validation...");

            // 1. JWT secret must not be empty or a dev placeholder
            if (jwtSecret == null || jwtSecret.isBlank()) {
                throw new IllegalStateException(
                        "FATAL: JWT_SECRET is not set. Cannot start in production without a signing key.");
            }
            if (jwtSecret.contains("dev-only") || jwtSecret.contains("not-for-production")) {
                throw new IllegalStateException(
                        "FATAL: JWT_SECRET contains a development placeholder. "
                                + "Generate a production key: openssl rand -base64 64");
            }
            if (jwtSecret.length() < 32) {
                throw new IllegalStateException(
                        "FATAL: JWT_SECRET is too short (minimum 32 characters for production).");
            }

            // 2. Database must not point to localhost
            if (datasourceUrl != null && datasourceUrl.contains("localhost")) {
                throw new IllegalStateException(
                        "FATAL: SPRING_DATASOURCE_URL points to localhost. "
                                + "Production must use a remote database.");
            }

            // 3. CORS must not include localhost origins
            if (corsOrigins != null && corsOrigins.contains("localhost")) {
                log.warn("CORS_ALLOWED_ORIGINS contains 'localhost'. "
                        + "This is unusual for production — verify this is intentional.");
            }

            // 4. Warn about optional but recommended integrations
            String smtpHost = System.getenv("SMTP_HOST");
            if (smtpHost == null || smtpHost.isBlank()) {
                log.warn("SMTP_HOST is not set — email notifications will be disabled.");
            }

            String r2AccountId = System.getenv("R2_ACCOUNT_ID");
            if (r2AccountId == null || r2AccountId.isBlank()) {
                log.warn("R2_ACCOUNT_ID is not set — cloud file storage will be disabled.");
            }

            log.info("Production environment validation PASSED.");
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Masks the password portion of a JDBC URL for safe logging.
     * e.g., jdbc:postgresql://user:secret@host:5432/db → jdbc:postgresql://user:****@host:5432/db
     */
    private String maskJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return "(not set)";
        }
        return url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }
}

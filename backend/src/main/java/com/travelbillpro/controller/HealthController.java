package com.travelbillpro.controller;

import com.travelbillpro.client.PythonExtractionClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Slf4j
public class HealthController {

    private final DataSource dataSource;
    private final PythonExtractionClient pythonClient;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public HealthController(DataSource dataSource, PythonExtractionClient pythonClient) {
        this.dataSource = dataSource;
        this.pythonClient = pythonClient;
    }

    /**
     * Basic liveness probe — always returns 200 if the JVM is running.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "travelbilling-backend",
                "timestamp", Instant.now().toString()
        );
    }

    /**
     * Readiness probe — checks all dependencies (DB, Redis, PDF extractor).
     * Returns 200 if all healthy, 503 if any are degraded.
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean allHealthy = true;

        // Database check
        try (var conn = dataSource.getConnection()) {
            try (var stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
            checks.put("database", "ok");
        } catch (Exception e) {
            checks.put("database", "failed: " + e.getMessage());
            allHealthy = false;
            log.warn("Health check: database failed - {}", e.getMessage());
        }

        // Redis check
        if (redisTemplate != null) {
            try {
                String pong = redisTemplate.getConnectionFactory().getConnection().ping();
                checks.put("redis", pong != null ? "ok" : "no response");
                if (pong == null) allHealthy = false;
            } catch (Exception e) {
                checks.put("redis", "failed: " + e.getMessage());
                allHealthy = false;
                log.warn("Health check: redis failed - {}", e.getMessage());
            }
        } else {
            checks.put("redis", "not configured");
        }

        // PDF extractor check
        try {
            boolean pdfHealthy = pythonClient.isHealthy();
            checks.put("pdf_extractor", pdfHealthy ? "ok" : "unreachable");
            if (!pdfHealthy) allHealthy = false;
        } catch (Exception e) {
            checks.put("pdf_extractor", "failed: " + e.getMessage());
            allHealthy = false;
        }

        checks.put("status", allHealthy ? "ready" : "degraded");
        checks.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(allHealthy ? 200 : 503).body(checks);
    }
}

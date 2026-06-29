package com.travelbillpro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "database_health_checks")
@Getter
@Setter
public class DatabaseHealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "database_id", nullable = false, length = 100)
    private String databaseId;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "org_name")
    private String orgName;

    @Column(name = "org_slug", length = 100)
    private String orgSlug;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @PrePersist
    protected void onCreate() {
        if (checkedAt == null) {
            checkedAt = LocalDateTime.now();
        }
    }
}

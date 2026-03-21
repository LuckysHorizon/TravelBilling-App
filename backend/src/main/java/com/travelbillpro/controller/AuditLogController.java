package com.travelbillpro.controller;

import com.travelbillpro.entity.AuditLog;
import com.travelbillpro.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getAuditLogs(Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);
        Page<Map<String, Object>> response = logs.map(this::mapToResponse);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapToResponse(AuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("action", log.getAction());
        map.put("entityType", log.getEntityType());
        map.put("entityId", log.getEntityId());
        map.put("user", log.getUser() != null ? log.getUser().getUsername() : "System");
        map.put("timestamp", log.getCreatedAt());
        map.put("ipAddress", log.getIpAddress());
        map.put("details", log.getNewValue() != null ? log.getNewValue() : log.getOldValue());
        return map;
    }
}

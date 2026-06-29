package com.travelbillpro.controller;

import com.travelbillpro.dto.KeepAliveDto.KeepAliveResult;
import com.travelbillpro.dto.KeepAliveDto.KeepAliveStatusResponse;
import com.travelbillpro.service.KeepAliveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Internal admin endpoints for database keep-alive management.
 * Restricted to SUPER_ADMIN users only.
 */
@RestController
@RequestMapping("/api/internal/keepalive")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class KeepAliveController {

    private final KeepAliveService keepAliveService;

    /**
     * POST /api/internal/keepalive/run
     * Manually trigger a keep-alive execution across all databases.
     */
    @PostMapping("/run")
    public ResponseEntity<KeepAliveResult> triggerKeepAlive() {
        log.info("[KEEPALIVE] Manual execution triggered via API");
        KeepAliveResult result = keepAliveService.executeKeepAlive();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/internal/keepalive/status
     * Return the status of the most recent keep-alive execution
     * along with per-database health information.
     */
    @GetMapping("/status")
    public ResponseEntity<KeepAliveStatusResponse> getStatus() {
        KeepAliveStatusResponse status = keepAliveService.getStatus();
        return ResponseEntity.ok(status);
    }
}

package com.travelbillpro.controller;

import com.travelbillpro.dto.AgentDto;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.service.AgentRateLimitService;
import com.travelbillpro.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final AgentRateLimitService rateLimitService;

    /**
     * Stream a chat response from the AI agent via Server-Sent Events.
     * Creates or continues a session based on the sessionId in the request.
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AgentDto.ChatRequest request,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        rateLimitService.checkRateLimit(userDetails.getUser().getId());
        SseEmitter emitter = new SseEmitter(60000L); // 60s timeout

        // Capture tenant context from the request thread (ThreadLocal-based)
        final Long tenantOrgId = com.travelbillpro.config.TenantContext.getOrgId();
        final String tenantDbUrl = com.travelbillpro.config.TenantContext.getDbUrl();
        final String tenantOrgSlug = com.travelbillpro.config.TenantContext.getOrgSlug();

        // Run in async thread to allow SSE streaming
        CompletableFuture.runAsync(() -> {
            try {
                // Re-establish tenant context in the async thread
                if (tenantOrgId != null) {
                    com.travelbillpro.config.TenantContext.setOrgId(tenantOrgId);
                }
                if (tenantDbUrl != null) {
                    com.travelbillpro.config.TenantContext.setDbUrl(tenantDbUrl);
                }
                if (tenantOrgSlug != null) {
                    com.travelbillpro.config.TenantContext.setOrgSlug(tenantOrgSlug);
                }

                agentService.streamChat(
                        request.getSessionId(),
                        request.getMessage(),
                        request.getContext(),
                        userDetails.getUser(),
                        emitter
                );
            } catch (Exception e) {
                log.error("Agent chat error", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("type", "error", "content", e.getMessage())));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            } finally {
                // Clean up tenant context in the async thread
                com.travelbillpro.config.TenantContext.clear();
            }
        });

        return emitter;
    }

    /**
     * List chat sessions for the authenticated user, ordered by most recent.
     */
    @GetMapping("/sessions")
    public ResponseEntity<Page<AgentDto.SessionResponse>> getSessions(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(agentService.getSessions(userDetails.getUser().getId(), pageable));
    }

    /**
     * Get messages for a specific session, ordered by most recent first.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Page<AgentDto.MessageResponse>> getMessages(
            @PathVariable UUID sessionId,
            Pageable pageable) {
        return ResponseEntity.ok(agentService.getMessages(sessionId, pageable));
    }

    /**
     * Delete a chat session. Only the owning user can delete their session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        agentService.deleteSession(sessionId, userDetails.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Execute a tool that was paused for user approval.
     * Called by the frontend after the user approves a destructive action.
     */
    @PostMapping("/tool/execute")
    public ResponseEntity<AgentDto.ToolResult> executeTool(
            @RequestBody AgentDto.ToolExecuteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                agentService.executeTool(request.getSessionId(), request.getToolName(),
                        request.getParameters(), userDetails.getUser())
        );
    }
}

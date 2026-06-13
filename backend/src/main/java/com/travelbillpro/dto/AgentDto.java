package com.travelbillpro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AgentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private UUID sessionId;
        private String message;
        private Map<String, Object> context;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatEvent {
        private String type; // "text", "tool_call", "approval_required", "error", "done"
        private String content;
        private Map<String, Object> toolCall;
        private Map<String, Object> usage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionResponse {
        private UUID sessionId;
        private String title;
        private Integer messageCount;
        private Integer totalTokens;
        private String providerUsed;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private UUID messageId;
        private String role;
        private String content;
        private Map<String, Object> toolCalls;
        private Map<String, Object> toolResults;
        private Integer tokenCount;
        private String provider;
        private String model;
        private Integer latencyMs;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolExecuteRequest {
        private UUID sessionId;
        private String toolName;
        private Map<String, Object> parameters;
        private boolean approved;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private boolean success;
        private Object data;
        private String message;
    }
}

package com.travelbillpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbillpro.client.LLMServiceClient;
import com.travelbillpro.config.AgentConfig;
import com.travelbillpro.dto.AgentDto;
import com.travelbillpro.entity.AgentMessage;
import com.travelbillpro.entity.AgentSession;
import com.travelbillpro.entity.User;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.AgentMessageRepository;
import com.travelbillpro.repository.AgentSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Orchestrates the AI agent chat workflow.
 * Manages sessions, message history, LLM interaction, tool execution, and SSE streaming.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentSessionRepository sessionRepository;
    private final AgentMessageRepository messageRepository;
    private final LLMServiceClient llmServiceClient;
    private final AgentToolExecutor toolExecutor;
    private final AgentConfig agentConfig;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are TravelBill AI, a friendly and intelligent assistant for the TravelBilling application.
            You help users manage travel tickets, invoices, companies, and billing.

            Current user: %s (Role: %s)
            Organization: %s
            Current page: %s

            IMPORTANT INSTRUCTIONS:
            1. For general conversation (greetings, questions about yourself, small talk), respond naturally and conversationally WITHOUT using any tools. Just reply directly.
            2. Only use tools when the user specifically asks about data (tickets, invoices, companies, stats) or wants to navigate somewhere.
            3. When you DO use tools to fetch data, summarize the results in a clear, human-readable way. Don't dump raw JSON.
            4. If the user asks to navigate, use the navigate_to tool and confirm where you're taking them.
            5. Be concise, helpful, and professional. Use formatting like bullet points and bold for readability.
            6. Never make up data — use tools to get real results when asked about application data.
            7. If asked to perform a destructive action, confirm with the user first.

            Available tools (use ONLY when needed for data/navigation):
            - get_tickets: Search and filter tickets
            - get_companies: List companies
            - get_invoices: Search invoices
            - get_dashboard_stats: Get summary statistics
            - navigate_to: Navigate the user to a specific page
            - get_current_context: Get current user information
            """;

    // ═══════════════════════════════════════════════════════════════════════
    //  Main Chat Entry Point (SSE Streaming)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Main entry point for streaming chat.
     * Implements an agentic loop: LLM → tool calls → execute → feed back → LLM → final text.
     * Max 3 tool-use iterations to prevent infinite loops.
     */
    public void streamChat(UUID sessionId, String userMessage, Map<String, Object> context,
                           User user, SseEmitter emitter) {
        try {
            // 1. Find or create session
            AgentSession session = findOrCreateSession(sessionId, user);
            UUID activeSessionId = session.getSessionId();

            // Send session info immediately
            sendEvent(emitter, AgentDto.ChatEvent.builder()
                    .type("session")
                    .content(activeSessionId.toString())
                    .build());

            // 2. Save user message
            saveMessage(activeSessionId, "user", userMessage, null, null, 0, null, null, null);

            // 3. Build message history from DB
            List<Map<String, Object>> messageHistory = buildMessageHistory(activeSessionId, context, user);
            messageHistory.add(Map.of("role", "user", "content", userMessage));

            // 4. Get tool schemas
            List<Map<String, Object>> toolSchemas = toolExecutor.getToolSchemas();

            long startMs = System.currentTimeMillis();
            int maxIterations = 3;

            // ═══ AGENTIC LOOP ═══
            // Each iteration: call LLM → if tool calls, execute and loop; if text, stream and exit
            for (int iteration = 0; iteration < maxIterations; iteration++) {
                log.info("Agent loop iteration {} for session {}", iteration + 1, activeSessionId);

                // Use non-streaming call to check if the LLM wants to call tools
                Map<String, Object> llmResponse = llmServiceClient.chat(messageHistory, toolSchemas);

                String content = (String) llmResponse.get("content");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) llmResponse.get("tool_calls");

                // If LLM returned text (no tool calls), stream it to the user
                if (toolCalls == null || toolCalls.isEmpty()) {
                    if (content != null && !content.isBlank()) {
                        // Stream the text in chunks for smooth UX
                        streamTextToEmitter(emitter, content);

                        // Save assistant message
                        long latencyMs = System.currentTimeMillis() - startMs;
                        saveMessage(activeSessionId, "assistant", content,
                                null, null, 0, "groq", null, (int) latencyMs);
                    }
                    break; // Exit loop — we have the final text response
                }

                // ── LLM wants to call tools ──
                // Add assistant's tool_call message to history
                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                if (content != null) assistantMsg.put("content", content);
                assistantMsg.put("tool_calls", toolCalls);
                messageHistory.add(assistantMsg);

                // Execute each tool and add results to history
                for (Map<String, Object> tc : toolCalls) {
                    String tcId = (String) tc.get("id");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> function = (Map<String, Object>) tc.get("function");
                    if (function == null) continue;

                    String toolName = (String) function.get("name");
                    String argsStr = (String) function.get("arguments");

                    // Notify frontend about tool execution
                    sendEvent(emitter, AgentDto.ChatEvent.builder()
                            .type("tool_call")
                            .content("🔧 Using: " + toolName)
                            .build());

                    // Parse arguments
                    Map<String, Object> toolParams = Map.of();
                    if (argsStr != null && !argsStr.isBlank()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsed = MAPPER.readValue(argsStr, Map.class);
                            toolParams = parsed;
                        } catch (Exception e) {
                            log.warn("Failed to parse tool arguments: {}", argsStr);
                        }
                    }

                    // Check approval
                    if (toolExecutor.isApprovalRequired(toolName)) {
                        Map<String, Object> approvalData = new LinkedHashMap<>();
                        approvalData.put("name", toolName);
                        approvalData.put("params", toolParams);
                        sendEvent(emitter, AgentDto.ChatEvent.builder()
                                .type("approval_required")
                                .content("Tool '" + toolName + "' requires approval.")
                                .toolCall(approvalData)
                                .build());
                        // Can't continue the loop — need user approval
                        // Save partial state and exit
                        sendDoneEvent(emitter, activeSessionId);
                        emitter.complete();
                        return;
                    }

                    // Execute tool
                    AgentDto.ToolResult result = toolExecutor.executeTool(toolName, toolParams, user);
                    String resultJson = result.getData() != null
                            ? MAPPER.writeValueAsString(result.getData()) : "No data";

                    // If this was a navigate_to tool, send a navigate event to frontend
                    if ("navigate_to".equals(toolName) && result.isSuccess() && result.getData() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> navData = (Map<String, Object>) result.getData();
                        String route = (String) navData.get("route");
                        if (route != null) {
                            sendEvent(emitter, AgentDto.ChatEvent.builder()
                                    .type("navigate")
                                    .content(route)
                                    .build());
                        }
                    }

                    // Add tool result to message history (OpenAI format)
                    Map<String, Object> toolResultMsg = new LinkedHashMap<>();
                    toolResultMsg.put("role", "tool");
                    toolResultMsg.put("tool_call_id", tcId != null ? tcId : toolName);
                    toolResultMsg.put("content", resultJson);
                    messageHistory.add(toolResultMsg);

                    log.info("Tool '{}' executed successfully, result length: {}", toolName, resultJson.length());
                }

                // Loop continues — next iteration will call LLM with tool results
            }

            // ── Post-loop: finalize session ──
            updateSessionMessageCount(activeSessionId);

            if (session.getTitle() == null || session.getTitle().isBlank()) {
                updateSessionTitle(activeSessionId, generateSessionTitle(userMessage));
            }

            sendDoneEvent(emitter, activeSessionId);
            emitter.complete();

        } catch (Exception e) {
            log.error("Stream chat error for session {}: {}", sessionId, e.getMessage(), e);
            try {
                sendEvent(emitter, AgentDto.ChatEvent.builder()
                        .type("error")
                        .content("Chat error: " + e.getMessage())
                        .build());
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }
    }

    /**
     * Stream text content to the SSE emitter in small chunks for smooth UX.
     */
    private void streamTextToEmitter(SseEmitter emitter, String text) throws IOException {
        // Stream in ~20 char chunks with small delays for typing effect
        int chunkSize = 20;
        for (int i = 0; i < text.length(); i += chunkSize) {
            String chunk = text.substring(i, Math.min(i + chunkSize, text.length()));
            sendEvent(emitter, AgentDto.ChatEvent.builder()
                    .type("text")
                    .content(chunk)
                    .build());
        }
    }

    /**
     * Send the final "done" event with session metadata.
     */
    private void sendDoneEvent(SseEmitter emitter, UUID sessionId) throws IOException {
        Map<String, Object> doneData = new LinkedHashMap<>();
        doneData.put("sessionId", sessionId.toString());
        doneData.put("messageId", "msg-" + System.currentTimeMillis());
        sendEvent(emitter, AgentDto.ChatEvent.builder()
                .type("done")
                .content(sessionId.toString())
                .usage(doneData)
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Session CRUD
    // ═══════════════════════════════════════════════════════════════════════

    public Page<AgentDto.SessionResponse> getSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(this::mapToSessionResponse);
    }

    public Page<AgentDto.MessageResponse> getMessages(UUID sessionId, Pageable pageable) {
        return messageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable)
                .map(this::mapToMessageResponse);
    }

    public void deleteSession(UUID sessionId, Long userId) {
        AgentSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found",
                        "SESSION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException("Cannot delete another user's session",
                    "UNAUTHORIZED", HttpStatus.FORBIDDEN);
        }

        sessionRepository.deleteBySessionId(sessionId);
        log.info("Deleted agent session: {}", sessionId);
    }

    /**
     * Execute a tool that was previously paused for approval.
     */
    public AgentDto.ToolResult executeTool(UUID sessionId, String toolName,
                                           Map<String, Object> params, User user) {
        // Verify session exists and belongs to user
        AgentSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found",
                        "SESSION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Cannot execute tools in another user's session",
                    "UNAUTHORIZED", HttpStatus.FORBIDDEN);
        }

        AgentDto.ToolResult result = toolExecutor.executeTool(toolName, params, user);

        // Save the tool execution as messages
        saveMessage(sessionId, "tool",
                "Tool executed: " + toolName,
                Map.of("name", toolName, "arguments", params),
                Map.of("success", result.isSuccess(), "data", result.getData() != null ? result.getData() : "null"),
                0, null, null, null);

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Internal Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private AgentSession findOrCreateSession(UUID sessionId, User user) {
        if (sessionId != null) {
            Optional<AgentSession> existing = sessionRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                AgentSession session = existing.get();
                session.setUpdatedAt(LocalDateTime.now());
                return sessionRepository.save(session);
            }
        }

        // Create new session
        AgentSession session = new AgentSession();
        session.setUser(user);
        session.setMetadata(Map.of());
        AgentSession saved = sessionRepository.save(session);
        log.info("Created new agent session: {} for user: {}", saved.getSessionId(), user.getUsername());
        return saved;
    }

    private List<Map<String, Object>> buildMessageHistory(UUID sessionId,
                                                           Map<String, Object> context, User user) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt with user context
        String currentPage = context != null ? (String) context.getOrDefault("currentPage", "unknown") : "unknown";
        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : "N/A";
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                user.getUsername(), user.getRole().name(), orgName, currentPage);

        messages.add(Map.of("role", "system", "content", systemPrompt));

        // Load recent messages from DB (reversed to chronological order)
        List<AgentMessage> recentMessages = messageRepository
                .findTop20BySessionIdOrderByCreatedAtDesc(sessionId);
        Collections.reverse(recentMessages);

        for (AgentMessage msg : recentMessages) {
            Map<String, Object> msgMap = new LinkedHashMap<>();
            msgMap.put("role", msg.getRole());
            if (msg.getContent() != null) {
                msgMap.put("content", msg.getContent());
            }
            if (msg.getToolCalls() != null) {
                msgMap.put("tool_calls", msg.getToolCalls());
            }
            if (msg.getToolResults() != null) {
                msgMap.put("tool_results", msg.getToolResults());
            }
            messages.add(msgMap);
        }

        return messages;
    }

    private void saveMessage(UUID sessionId, String role, String content,
                                     Map<String, Object> toolCalls, Map<String, Object> toolResults,
                                     int tokenCount, String provider, String model, Integer latencyMs) {
        try {
            AgentMessage message = new AgentMessage();
            message.setSessionId(sessionId);
            message.setRole(role);
            message.setContent(content);
            message.setToolCalls(toolCalls);
            message.setToolResults(toolResults);
            message.setTokenCount(tokenCount);
            message.setProvider(provider);
            message.setModel(model);
            message.setLatencyMs(latencyMs);
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("Failed to save agent message: {}", e.getMessage());
        }
    }

    private void updateSessionTokens(UUID sessionId, int tokens, String provider) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setTotalTokens(session.getTotalTokens() + tokens);
            if (provider != null) {
                session.setProviderUsed(provider);
            }
            sessionRepository.save(session);
        });
    }

    private void updateSessionMessageCount(UUID sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            long count = messageRepository.countBySessionId(sessionId);
            session.setMessageCount((int) count);
            sessionRepository.save(session);
        });
    }

    private void updateSessionTitle(UUID sessionId, String title) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setTitle(title);
            sessionRepository.save(session);
        });
    }

    /**
     * Generate a short title from the first user message.
     * Truncates to 100 chars and cleans up.
     */
    private String generateSessionTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return "New Chat";
        }
        String title = firstMessage.trim();
        // Remove line breaks
        title = title.replaceAll("[\\r\\n]+", " ");
        // Truncate
        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }
        return title;
    }

    private void sendEvent(SseEmitter emitter, AgentDto.ChatEvent event) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event.getType())
                .data(event));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Mappers
    // ═══════════════════════════════════════════════════════════════════════

    private AgentDto.SessionResponse mapToSessionResponse(AgentSession session) {
        return AgentDto.SessionResponse.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .messageCount(session.getMessageCount())
                .totalTokens(session.getTotalTokens())
                .providerUsed(session.getProviderUsed())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private AgentDto.MessageResponse mapToMessageResponse(AgentMessage message) {
        return AgentDto.MessageResponse.builder()
                .messageId(message.getMessageId())
                .role(message.getRole())
                .content(message.getContent())
                .toolCalls(message.getToolCalls())
                .toolResults(message.getToolResults())
                .tokenCount(message.getTokenCount())
                .provider(message.getProvider())
                .model(message.getModel())
                .latencyMs(message.getLatencyMs())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

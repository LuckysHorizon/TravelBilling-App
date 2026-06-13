package com.travelbillpro.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbillpro.config.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP client that calls the agent-llm Python service for AI chat completions.
 * Supports both synchronous and SSE streaming responses.
 */
@Component
public class LLMServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentConfig agentConfig;
    private final HttpClient httpClient;

    public LLMServiceClient(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Send a non-streaming chat request to the LLM service.
     *
     * @param messages list of message maps with "role" and "content" keys
     * @param tools    list of tool schema maps in OpenAI function-calling format
     * @return parsed response map from the LLM service
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            long startMs = System.currentTimeMillis();

            Map<String, Object> requestBody = Map.of(
                    "messages", messages,
                    "tools", tools != null ? tools : List.of(),
                    "stream", false
            );

            String jsonBody = MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentConfig.getLlmServiceUrl() + "/v1/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(agentConfig.getStreamTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long latencyMs = System.currentTimeMillis() - startMs;
            log.info("LLM chat response: status={}, latency={}ms, bodyLength={}",
                    response.statusCode(), latencyMs, response.body().length());

            if (response.statusCode() != 200) {
                throw new RuntimeException("LLM service returned HTTP " + response.statusCode()
                        + ": " + truncate(response.body(), 300));
            }

            Map<String, Object> result = MAPPER.readValue(response.body(), new TypeReference<>() {});
            result.put("_latency_ms", latencyMs);
            return result;

        } catch (RuntimeException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("LLM service is not running at " + agentConfig.getLlmServiceUrl()
                    + ". Start it with: cd agent-llm && python main.py", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call LLM service: " + e.getMessage(), e);
        }
    }

    /**
     * Send a streaming chat request to the LLM service.
     * The LLM service returns Server-Sent Events (SSE), each chunk forwarded to the consumer.
     *
     * @param messages list of message maps with "role" and "content" keys
     * @param tools    list of tool schema maps in OpenAI function-calling format
     * @param onChunk  consumer that receives each SSE data line as a raw string
     */
    public void chatStream(List<Map<String, Object>> messages, List<Map<String, Object>> tools,
                           Consumer<String> onChunk) {
        try {
            long startMs = System.currentTimeMillis();

            Map<String, Object> requestBody = Map.of(
                    "messages", messages,
                    "tools", tools != null ? tools : List.of(),
                    "stream", true
            );

            String jsonBody = MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentConfig.getLlmServiceUrl() + "/v1/chat/stream"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(agentConfig.getStreamTimeoutSeconds()))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("LLM stream returned HTTP " + response.statusCode()
                        + ": " + truncate(errorBody, 300));
            }

            // Read SSE stream line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            log.debug("SSE stream completed in {}ms", System.currentTimeMillis() - startMs);
                            break;
                        }
                        onChunk.accept(data);
                    }
                    // Skip empty lines and "event:" lines (standard SSE format)
                }
            }

            log.info("LLM stream completed: latency={}ms", System.currentTimeMillis() - startMs);

        } catch (RuntimeException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("LLM service is not running at " + agentConfig.getLlmServiceUrl()
                    + ". Start it with: cd agent-llm && python main.py", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream from LLM service: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the LLM service is healthy and reachable.
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(agentConfig.getLlmServiceUrl() + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("LLM service health check failed: {}", e.getMessage());
            return false;
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}

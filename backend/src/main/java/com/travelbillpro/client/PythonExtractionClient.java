package com.travelbillpro.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbillpro.exception.ExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client that calls the Python PDF extraction microservice.
 * Java owns persistence. Python owns extraction.
 *
 * Python service renders PDF pages with PyMuPDF and calls NVIDIA vision API.
 * This client sends the absolute file path and receives structured JSON records.
 */
@Component
public class PythonExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(PythonExtractionClient.class);

    @Value("${extraction.service.url:http://localhost:8000/extract}")
    private String serviceUrl;

    @Value("${extraction.service.health-url:http://localhost:8000/health}")
    private String healthUrl;

    @Value("${extraction.service.timeout-seconds:120}")
    private int timeoutSeconds;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)  // Force HTTP/1.1 — uvicorn basic doesn't support h2c upgrade
            .build();

    /**
     * Call the Python service to extract ticket data from a PDF.
     *
     * @param absoluteFilePath the path where Java already saved the PDF
     * @param companyId        from auth context
     * @return parsed extraction response as a Map containing status, records, model info, and token usage
     * @throws ExtractionException on any failure
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extract(String absoluteFilePath, Long companyId) {
        try {
            Map<String, Object> body = Map.of(
                "file_path", absoluteFilePath,
                "company_id", companyId
            );
            String bodyJson = MAPPER.writeValueAsString(body);

            log.info("Calling Python extraction service: file={}, company={}", 
                    absoluteFilePath, companyId);
            log.debug("Request body JSON: {}", bodyJson);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, java.nio.charset.StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("Python service responded with HTTP {}", response.statusCode());

            if (response.statusCode() == 404) {
                throw new ExtractionException(
                    "PDF file not found by Python service: " + absoluteFilePath);
            }
            if (response.statusCode() == 422) {
                Map<String, Object> errorResp = MAPPER.readValue(response.body(),
                        new TypeReference<>() {});
                // Python FastAPI returns "detail" for validation errors, our handler returns "error"
                Object detail = errorResp.get("detail");
                if (detail == null) detail = errorResp.get("error");
                if (detail == null) detail = response.body().substring(0, Math.min(200, response.body().length()));
                throw new ExtractionException("Python extraction failed: " + detail);
            }
            if (response.statusCode() != 200) {
                throw new ExtractionException(
                    "Python service returned HTTP " + response.statusCode()
                    + ": " + response.body().substring(0, Math.min(200, response.body().length())));
            }

            Map<String, Object> resp = MAPPER.readValue(response.body(),
                    new TypeReference<>() {});

            String status = (String) resp.get("status");
            log.info("Python service returned status={}, total_passengers={}",
                    status, resp.get("total_passengers"));

            if ("FAILED".equals(status)) {
                throw new ExtractionException(
                    "Python extraction returned FAILED: " + resp.get("error"));
            }

            return resp;

        } catch (ExtractionException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new ExtractionException(
                "Python extraction service is not running on " + serviceUrl
                + ". Start it with: cd pdf-extractor && uvicorn main:app --port 8000");
        } catch (Exception e) {
            throw new ExtractionException(
                "Failed to call Python extraction service: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the Python service is healthy. Returns true if reachable.
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Python extraction service health check failed: {}", e.getMessage());
            return false;
        }
    }
}

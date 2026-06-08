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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP client that calls the Python PDF extraction microservice.
 * Java owns persistence. Python owns extraction.
 *
 * Sends PDF bytes via multipart/form-data to avoid cross-container
 * filesystem path issues in Docker environments.
 */
@Component
public class PythonExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(PythonExtractionClient.class);

    @Value("${extraction.service.url:http://localhost:8000/extract-upload}")
    private String serviceUrl;

    @Value("${extraction.service.health-url:http://localhost:8000/health}")
    private String healthUrl;

    @Value("${extraction.service.timeout-seconds:120}")
    private int timeoutSeconds;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * Call the Python service to extract ticket data from PDF bytes.
     *
     * Sends the PDF as multipart/form-data so it works across Docker
     * containers without shared filesystems.
     *
     * @param pdfBytes   raw PDF file content
     * @param filename   original file name (for logging in Python)
     * @param companyId  from auth context
     * @return parsed extraction response as a Map
     * @throws ExtractionException on any failure
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extract(byte[] pdfBytes, String filename, Long companyId) {
        try {
            String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");

            byte[] body = buildMultipartBody(boundary, pdfBytes, filename, companyId);

            log.info("Calling Python extraction service: file={}, company={}, size={}bytes",
                    filename, companyId, pdfBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("Python service responded with HTTP {}", response.statusCode());

            if (response.statusCode() == 404) {
                throw new ExtractionException(
                    "PDF file not found by Python service: " + filename);
            }
            if (response.statusCode() == 422) {
                Map<String, Object> errorResp = MAPPER.readValue(response.body(),
                        new TypeReference<>() {});
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
     * Build multipart/form-data body with PDF file and metadata fields.
     */
    private byte[] buildMultipartBody(String boundary, byte[] pdfBytes, String filename, Long companyId) {
        String CRLF = "\r\n";
        StringBuilder sb = new StringBuilder();

        // Part 1: company_id field
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"company_id\"").append(CRLF);
        sb.append(CRLF);
        sb.append(companyId).append(CRLF);

        // Part 2: file field (header only — bytes appended separately)
        sb.append("--").append(boundary).append(CRLF);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
          .append(filename != null ? filename : "upload.pdf").append("\"").append(CRLF);
        sb.append("Content-Type: application/pdf").append(CRLF);
        sb.append(CRLF);

        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

        // Combine: headers + PDF bytes + footer
        byte[] result = new byte[headerBytes.length + pdfBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(pdfBytes, 0, result, headerBytes.length, pdfBytes.length);
        System.arraycopy(footerBytes, 0, result, headerBytes.length + pdfBytes.length, footerBytes.length);

        return result;
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

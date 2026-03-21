package com.travelbillpro.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.nvidia")
@Getter
@Setter
public class NvidiaConfig {

    private String apiKey;
    private String baseUrl = "https://integrate.api.nvidia.com/v1";
    private String modelText = "meta/llama-3.1-70b-instruct";
    private String modelVision = "meta/llama-3.2-90b-vision-instruct";
    private int maxTokens = 4096;
    private int timeoutSeconds = 90;
    private int rateLimitPerMinute = 38;
}

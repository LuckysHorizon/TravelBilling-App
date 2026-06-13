package com.travelbillpro.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "agent")
@Getter @Setter
public class AgentConfig {
    private String llmServiceUrl = "http://localhost:8001";
    private int rateLimitPerMinute = 20;
    private int rateLimitPerDay = 500;
    private int maxContextMessages = 20;
    private int streamTimeoutSeconds = 60;
}

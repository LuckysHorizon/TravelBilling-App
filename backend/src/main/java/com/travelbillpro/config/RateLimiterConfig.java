package com.travelbillpro.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    /**
     * NVIDIA NIM free tier: 40 req/min.
     * We use 38/min for safety margin.
     * RateLimiter.create takes permits-per-second: 38/60 ≈ 0.633
     */
    @Bean("nvidiaRateLimiter")
    public RateLimiter nvidiaRateLimiter() {
        return RateLimiter.create(38.0 / 60.0);
    }
}

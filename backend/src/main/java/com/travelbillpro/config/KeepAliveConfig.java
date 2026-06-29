package com.travelbillpro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for the Database Keep-Alive subsystem.
 * Enables scheduling and provides a bounded thread pool for parallel pings.
 */
@Configuration
@EnableScheduling
public class KeepAliveConfig {

    @Value("${app.keepalive.max-threads:5}")
    private int maxThreads;

    /**
     * Bounded thread pool for parallel database pings.
     * Limits concurrency to avoid opening too many connections simultaneously.
     */
    @Bean(name = "keepAliveExecutor", destroyMethod = "shutdown")
    public ExecutorService keepAliveExecutor() {
        return Executors.newFixedThreadPool(maxThreads, r -> {
            Thread t = new Thread(r, "keepalive-ping");
            t.setDaemon(true);
            return t;
        });
    }
}

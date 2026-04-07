package com.travelbillpro.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Slf4j
public class ProductionEnvSanityCheck {

    @Bean
    public ApplicationRunner renderEnvSanityRunner(@Value("${spring.datasource.url:}") String datasourceUrl) {
        return args -> {
            if (datasourceUrl != null && datasourceUrl.length() >= 2) {
                if ((datasourceUrl.startsWith("\"") && datasourceUrl.endsWith("\""))
                        || (datasourceUrl.startsWith("'") && datasourceUrl.endsWith("'"))) {
                    log.error("SPRING_DATASOURCE_URL appears to be wrapped in quotes. Remove quotes in Render env settings.");
                }
            }

            Map<String, String> env = System.getenv();
            if (env.containsKey("spring.datasource.hikari.maximum-pool-size")) {
                log.error("Detected unsupported env key 'spring.datasource.hikari.maximum-pool-size'. Use 'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE'.");
            }
            if (env.containsKey("spring.datasource.hikari.data-source-properties.prepareThreshold")) {
                log.error("Detected unsupported env key 'spring.datasource.hikari.data-source-properties.prepareThreshold'. Use 'SPRING_DATASOURCE_HIKARI_DATA_SOURCE_PROPERTIES_PREPARE_THRESHOLD'.");
            }
        };
    }
}

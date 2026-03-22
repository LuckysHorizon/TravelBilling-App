package com.travelbillpro.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configures the primary DataSource as a tenant-aware routing DataSource.
 *
 * - masterDataSource: the local PostgreSQL DB (configured in application.yml)
 * - tenantAwareDataSource: wraps master + dynamically routes to tenant DBs
 */
@Configuration
@Slf4j
public class DataSourceConfig {

    /**
     * Parse spring.datasource.* properties.
     */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties masterDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Create the master (local) DataSource from application.yml config.
     * This is NOT @Primary — the TenantAwareDataSource wrapping it is.
     */
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource masterDataSource(DataSourceProperties masterDataSourceProperties) {
        HikariDataSource ds = masterDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setPoolName("master-pool");
        log.info("Master DataSource created: {}", ds.getJdbcUrl());
        return ds;
    }

    /**
     * The PRIMARY DataSource bean — all JPA/Hibernate/Spring operations use this.
     * Routes to tenant DB when TenantContext is set, otherwise master DB.
     */
    @Bean
    @Primary
    public DataSource dataSource(HikariDataSource masterDataSource, TenantDataSourceManager tenantDataSourceManager) {
        log.info("Creating TenantAwareDataSource (primary) wrapping master pool");
        return new TenantAwareDataSource(masterDataSource, tenantDataSourceManager);
    }
}

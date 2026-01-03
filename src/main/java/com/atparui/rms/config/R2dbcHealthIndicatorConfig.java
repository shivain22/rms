package com.atparui.rms.config;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.r2dbc.ConnectionFactoryHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

/**
 * Advanced configuration for R2DBC health checks in a multi-tenant setup.
 *
 * This configuration ensures that:
 * 1. Only the correct ConnectionFactory (healthCheckConnectionFactory) is health-checked
 * 2. Other ConnectionFactory beans (like masterConnectionFactory) are excluded from health checks
 * 3. Spring Actuator's auto-discovery is overridden to prevent checking all factories
 *
 * The healthCheckConnectionFactory is configured with the correct Docker service name
 * (rms-postgresql) instead of localhost.
 */
@Configuration
@ConditionalOnEnabledHealthIndicator("r2dbc")
public class R2dbcHealthIndicatorConfig {

    private static final Logger log = LoggerFactory.getLogger(R2dbcHealthIndicatorConfig.class);

    /**
     * Explicitly configure the ConnectionFactoryHealthIndicator to use ONLY the
     * dedicated healthCheckConnectionFactory bean. This overrides Spring Boot's
     * auto-configuration which would discover ALL ConnectionFactory beans.
     *
     * By marking this as @Primary, we ensure that:
     * - This is the PRIMARY ConnectionFactoryHealthIndicator in the context
     * - Spring Actuator will use this instead of auto-creating indicators for other factories
     * - Only the healthCheckConnectionFactory (with correct Docker hostname) is checked
     *
     * The @ConditionalOnMissingBean prevents conflicts if another indicator is already defined,
     * but our @Primary ensures this takes precedence.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ConnectionFactoryHealthIndicator.class)
    @DependsOn("healthCheckConnectionFactory")
    public ConnectionFactoryHealthIndicator connectionFactoryHealthIndicator(
        @Qualifier("healthCheckConnectionFactory") ConnectionFactory connectionFactory
    ) {
        log.info("=== Configuring R2DBC Health Indicator ===");
        log.info("Using ConnectionFactory: {}", connectionFactory);
        log.info("ConnectionFactory class: {}", connectionFactory.getClass().getName());

        // Log connection details if it's a PostgresqlConnectionFactory
        if (connectionFactory instanceof io.r2dbc.postgresql.PostgresqlConnectionFactory) {
            try {
                var metadata = connectionFactory.getMetadata();
                log.info("ConnectionFactory metadata: {}", metadata);
            } catch (Exception e) {
                log.warn("Could not get ConnectionFactory metadata: {}", e.getMessage());
            }
        }

        log.info("Health indicator will use ConnectionFactory with host from DB_HOST: {}", System.getenv("DB_HOST"));

        // Try to extract host information from PostgresqlConnectionFactory
        if (connectionFactory instanceof io.r2dbc.postgresql.PostgresqlConnectionFactory) {
            try {
                // Use reflection to get the configuration
                var configField = connectionFactory.getClass().getDeclaredField("configuration");
                configField.setAccessible(true);
                var config = configField.get(connectionFactory);
                var hostMethod = config.getClass().getMethod("getHost");
                var host = hostMethod.invoke(config);
                log.info("ConnectionFactory host from configuration: {}", host);
                if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
                    log.error("ERROR: ConnectionFactory is configured with localhost! Expected: postgresql");
                }
            } catch (Exception e) {
                log.warn("Could not extract host from ConnectionFactory: {}", e.getMessage());
            }
        }

        log.info("=========================================");
        return new ConnectionFactoryHealthIndicator(connectionFactory);
    }
}

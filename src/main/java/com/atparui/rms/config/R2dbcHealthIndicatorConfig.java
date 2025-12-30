package com.atparui.rms.config;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.r2dbc.ConnectionFactoryHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to ensure R2DBC health check uses the @Primary ConnectionFactory
 * which is configured with the correct Docker service name (rms-postgresql).
 *
 * This explicitly configures the health indicator to use the @Primary ConnectionFactory
 * bean from MultiTenantDatabaseConfig, ensuring it uses rms-postgresql instead of localhost.
 */
@Configuration
@ConditionalOnEnabledHealthIndicator("r2dbc")
public class R2dbcHealthIndicatorConfig {

    private static final Logger log = LoggerFactory.getLogger(R2dbcHealthIndicatorConfig.class);

    /**
     * Explicitly configure the ConnectionFactoryHealthIndicator to use the
     * @Primary ConnectionFactory bean. This ensures the health check uses
     * the correct database connection (rms-postgresql) instead of localhost.
     *
     * The @Primary annotation ensures this bean takes precedence over any
     * auto-configured ConnectionFactoryHealthIndicator.
     */
    @Bean
    @Primary
    @DependsOn("connectionFactory")
    public ConnectionFactoryHealthIndicator connectionFactoryHealthIndicator(ConnectionFactory connectionFactory) {
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
        log.info("=========================================");
        return new ConnectionFactoryHealthIndicator(connectionFactory);
    }
}

package com.atparui.rms.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to remove R2DBC health indicators.
 * Since R2DBC health is disabled via management.health.r2dbc.enabled=false,
 * this class is kept for backward compatibility but may not be needed.
 *
 * Uses @PostConstruct to avoid circular dependency issues.
 */
@Configuration
public class ActuatorHealthOverrideConfig {

    private static final Logger log = LoggerFactory.getLogger(ActuatorHealthOverrideConfig.class);

    @Autowired(required = false)
    private HealthContributorRegistry healthContributorRegistry;

    @PostConstruct
    public void removeR2dbcHealthIndicators() {
        if (healthContributorRegistry != null) {
            try {
                // Remove R2DBC health indicators if they exist
                // Note: This may not be needed if management.health.r2dbc.enabled=false
                if (healthContributorRegistry.getContributor("r2dbc") != null) {
                    healthContributorRegistry.unregisterContributor("r2dbc");
                    log.info("Unregistered R2DBC health indicator");
                }
                if (healthContributorRegistry.getContributor("connectionFactory") != null) {
                    healthContributorRegistry.unregisterContributor("connectionFactory");
                    log.info("Unregistered ConnectionFactory health indicator");
                }
            } catch (Exception e) {
                log.warn("Could not unregister R2DBC health indicators: {}", e.getMessage());
            }
        }
    }
}

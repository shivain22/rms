package com.atparui.rms.config;

import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorHealthOverrideConfig {

    @Bean
    public HealthContributorRegistry healthContributorRegistry(HealthContributorRegistry registry) {
        // Remove ALL R2DBC ConnectionFactory health indicators
        registry.unregisterContributor("r2dbc");
        registry.unregisterContributor("connectionFactory");

        return registry;
    }
}

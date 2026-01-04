package com.atparui.rms.config;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Component to print all environment variables and Spring configuration properties
 * at application startup using System.out.println for debugging purposes.
 *
 * NOTE: Currently commented out to reduce startup log verbosity.
 * Uncomment @Component annotation below to re-enable configuration dumping.
 */
// @Component
public class StartupConfigPrinter {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void printConfiguration() {
        System.out.println("\n");
        System.out.println("===================================================================");
        System.out.println("=== SPRING APPLICATION CONFIGURATION DUMP ===");
        System.out.println("===================================================================");
        System.out.println("\n");

        // Print all environment variables
        System.out.println("--- ENVIRONMENT VARIABLES ---");
        Map<String, String> envVars = new TreeMap<>(System.getenv());
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("\n");

        // Print all system properties
        System.out.println("--- SYSTEM PROPERTIES ---");
        Properties systemProps = System.getProperties();
        TreeMap<String, String> sortedProps = new TreeMap<>();
        for (String key : systemProps.stringPropertyNames()) {
            sortedProps.put(key, systemProps.getProperty(key));
        }
        for (Map.Entry<String, String> entry : sortedProps.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("\n");

        // Print all Spring configuration properties
        System.out.println("--- SPRING CONFIGURATION PROPERTIES ---");
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;
            Map<String, Object> allProperties = new TreeMap<>();

            for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
                    for (String key : enumerablePropertySource.getPropertyNames()) {
                        try {
                            Object value = enumerablePropertySource.getProperty(key);
                            if (value != null) {
                                allProperties.put(key, value);
                            }
                        } catch (Exception e) {
                            // Ignore properties that can't be read
                        }
                    }
                }
            }

            for (Map.Entry<String, Object> entry : allProperties.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
        }
        System.out.println("\n");

        // Print specific management/actuator properties
        System.out.println("--- MANAGEMENT/ACTUATOR PROPERTIES ---");
        printProperty("management.endpoints.web.exposure.include");
        printProperty("management.endpoints.web.exposure.exclude");
        printProperty("management.health.r2dbc.enabled");
        printProperty("management.endpoint.env.enabled");
        printProperty("management.endpoint.configprops.enabled");
        printProperty("management.endpoint.health.enabled");
        printProperty("management.endpoints.web.base-path");
        System.out.println("\n");

        // Print database configuration
        System.out.println("--- DATABASE CONFIGURATION ---");
        printProperty("spring.r2dbc.url");
        printProperty("spring.r2dbc.username");
        printProperty("spring.r2dbc.password");
        printProperty("spring.datasource.url");
        printProperty("spring.datasource.username");
        printProperty("spring.datasource.password");
        printProperty("DB_HOST");
        printProperty("DB_PORT");
        printProperty("DB_NAME");
        printProperty("DB_USERNAME");
        printProperty("DB_PASSWORD");
        System.out.println("\n");

        // Print active profiles
        System.out.println("--- ACTIVE PROFILES ---");
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            System.out.println("Active Profile: " + profile);
        }
        System.out.println("\n");

        // Print all bean names (optional, can be verbose)
        System.out.println("--- BEAN COUNT ---");
        System.out.println("Total Beans: " + applicationContext.getBeanDefinitionCount());
        System.out.println("\n");

        System.out.println("===================================================================");
        System.out.println("=== END OF CONFIGURATION DUMP ===");
        System.out.println("===================================================================");
        System.out.println("\n");
    }

    private void printProperty(String key) {
        try {
            String value = environment.getProperty(key);
            if (value != null) {
                System.out.println(key + " = " + value);
            } else {
                System.out.println(key + " = [NOT SET]");
            }
        } catch (Exception e) {
            System.out.println(key + " = [ERROR: " + e.getMessage() + "]");
        }
    }
}

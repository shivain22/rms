package com.atparui.rms.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.atparui.rms.repository",
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*search.*")
)
@EnableTransactionManagement
public class MultiTenantDatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantDatabaseConfig.class);
    private final Map<String, ConnectionFactory> tenantConnectionFactories = new ConcurrentHashMap<>();

    @Value("${DB_HOST:postgresql}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${DB_USERNAME:rms_gateway}")
    private String dbUsername;

    @Value("${DB_PASSWORD:rms_gateway}")
    private String dbPassword;

    @Value("${DB_NAME:rms_gateway}")
    private String dbName;

    @PostConstruct
    public void initializeTenantConnections() {
        // Initialize connection to gateway database (rms_gateway)
        // Tenant information is stored in the tenants table in the gateway database
        // Each tenant may have its own database with different connection parameters
        log.info("=== MultiTenantDatabaseConfig Initialization ===");
        log.info("DB_HOST from @Value: {}", dbHost);
        log.info("DB_HOST from environment: {}", System.getenv("DB_HOST"));
        log.info("DB_PORT: {}", dbPort);
        log.info("DB_NAME: {}", dbName);
        log.info("DB_USERNAME: {}", dbUsername);
        log.info("Initialized database connection to: {} at {}:{}", dbName, dbHost, dbPort);
        log.info("================================================");
    }

    @Bean(name = "connectionFactory")
    @Primary
    public ConnectionFactory connectionFactory() {
        // Connection factory for the gateway database (rms_gateway)
        // This is the primary connection factory used by the application
        // Tenant-specific databases are accessed via TenantService based on tenant context
        log.info("=== Creating PRIMARY ConnectionFactory ===");
        log.info("Database: {} at {}:{}", dbName, dbHost, dbPort);
        log.info("DB_HOST from @Value: {}", dbHost);
        log.info("DB_HOST from environment: {}", System.getenv("DB_HOST"));
        log.info("DB_USERNAME: {}", dbUsername);

        if ("localhost".equals(dbHost) || "127.0.0.1".equals(dbHost)) {
            log.error("WARNING: DB_HOST is set to localhost! This will fail in Docker. Expected: postgresql");
        }

        ConnectionFactory factory = createTenantConnectionFactory(dbName);
        log.info("ConnectionFactory created successfully for host: {}", dbHost);
        log.info("==========================================");
        return factory;
    }

    /**
     * Dedicated ConnectionFactory for health checks.
     * This ensures the health indicator always uses the correct database connection
     * (postgresql service name in Docker) instead of falling back to localhost.
     */
    @Bean(name = "healthCheckConnectionFactory")
    public ConnectionFactory healthCheckConnectionFactory() {
        log.info("=== Creating Health Check ConnectionFactory ===");
        log.info("Database: {} at {}:{}", dbName, dbHost, dbPort);
        log.info("DB_HOST: {}", dbHost);

        if ("localhost".equals(dbHost) || "127.0.0.1".equals(dbHost)) {
            log.error("ERROR: DB_HOST is set to localhost! This will fail in Docker. Expected: postgresql");
        }

        ConnectionFactory factory = createTenantConnectionFactory(dbName);
        log.info("Health check ConnectionFactory created successfully for host: {}", dbHost);
        log.info("==========================================");
        return factory;
    }

    private ConnectionFactory createTenantConnectionFactory(String databaseName) {
        // Create a connection factory for the specified database
        // This is used for both the gateway database and tenant-specific databases
        log.info("Creating ConnectionFactory - host: {}, port: {}, database: {}, username: {}", dbHost, dbPort, databaseName, dbUsername);
        return new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(dbHost)
                .port(dbPort)
                .database(databaseName)
                .username(dbUsername)
                .password(dbPassword)
                .build()
        );
    }

    public void addTenantConnectionFactory(String tenantKey, ConnectionFactory connectionFactory) {
        tenantConnectionFactories.put(tenantKey, connectionFactory);
    }

    @Bean
    @Primary
    public R2dbcTransactionManager transactionManager() {
        return new R2dbcTransactionManager(connectionFactory());
    }
}

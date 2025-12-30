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
import org.springframework.core.annotation.Order;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@EnableR2dbcRepositories(
    basePackages = "com.atparui.rms.repository",
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*search.*")
)
@EnableTransactionManagement
public class MultiTenantDatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantDatabaseConfig.class);
    private final Map<String, ConnectionFactory> tenantConnectionFactories = new ConcurrentHashMap<>();

    @Value("${DB_HOST:rms-postgresql}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${DB_USERNAME:rms_gateway}")
    private String dbUsername;

    @Value("${DB_PASSWORD:rms_gateway}")
    private String dbPassword;

    @Value("${DB_NAME:rms_gateway}")
    private String dbName;

    @Value("${DB_SCHEMA:public}")
    private String dbSchema;

    @Value("${DB_TENANT1_NAME:rms_tenant1}")
    private String tenant1DbName;

    @Value("${DB_TENANT2_NAME:rms_tenant2}")
    private String tenant2DbName;

    @PostConstruct
    public void initializeTenantConnections() {
        // Simple initialization - all data goes to rms database
        // Tenant-specific databases will be created separately when needed
        log.info("=== MultiTenantDatabaseConfig Initialization ===");
        log.info("DB_HOST from @Value: {}", dbHost);
        log.info("DB_HOST from environment: {}", System.getenv("DB_HOST"));
        log.info("DB_PORT: {}", dbPort);
        log.info("DB_NAME: {}", dbName);
        log.info("DB_USERNAME: {}", dbUsername);
        log.info("DB_SCHEMA: {}", dbSchema);
        log.info("Initialized database connection to: {} at {}:{}", dbName, dbHost, dbPort);
        log.info("Using database credentials - username: {}, schema: {}", dbUsername, dbSchema);
        log.info("================================================");
    }

    @Bean(name = "connectionFactory")
    @Primary
    public ConnectionFactory connectionFactory() {
        // Use simple connection factory pointing to rms database
        // Tenant data is stored in the same database for now
        // This bean is marked @Primary to ensure it's used by application logic
        log.info("=== Creating PRIMARY ConnectionFactory ===");
        log.info("Database: {} at {}:{}", dbName, dbHost, dbPort);
        log.info("DB_HOST from @Value: {}", dbHost);
        log.info("DB_HOST from environment: {}", System.getenv("DB_HOST"));
        log.info("DB_USERNAME: {}", dbUsername);

        if ("localhost".equals(dbHost) || "127.0.0.1".equals(dbHost)) {
            log.error("WARNING: DB_HOST is set to localhost! This will fail in Docker. Expected: rms-postgresql");
        }

        ConnectionFactory factory = createTenantConnectionFactory(dbName);
        log.info("ConnectionFactory created successfully for host: {}", dbHost);
        log.info("==========================================");
        return factory;
    }

    /**
     * Dedicated ConnectionFactory for health checks.
     * This ensures the health indicator always uses the correct database connection
     * (rms-postgresql) instead of falling back to localhost.
     */
    @Bean(name = "healthCheckConnectionFactory")
    public ConnectionFactory healthCheckConnectionFactory() {
        log.info("=== Creating Health Check ConnectionFactory ===");
        log.info("Database: {} at {}:{}", dbName, dbHost, dbPort);
        log.info("DB_HOST: {}", dbHost);

        if ("localhost".equals(dbHost) || "127.0.0.1".equals(dbHost)) {
            log.error("ERROR: DB_HOST is set to localhost! This will fail in Docker. Expected: rms-postgresql");
        }

        ConnectionFactory factory = createTenantConnectionFactory(dbName);
        log.info("Health check ConnectionFactory created successfully for host: {}", dbHost);
        log.info("==========================================");
        return factory;
    }

    private ConnectionFactory createTenantConnectionFactory(String databaseName) {
        // Schema is set via SPRING_R2DBC_URL parameter (?schema=rms_gateway)
        // This manual connection factory is used for tenant-specific connections
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

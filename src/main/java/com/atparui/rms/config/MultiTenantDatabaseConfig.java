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

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${DB_USERNAME:rms}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${DB_NAME:rms}")
    private String dbName;

    @Value("${DB_TENANT1_NAME:rms_tenant1}")
    private String tenant1DbName;

    @Value("${DB_TENANT2_NAME:rms_tenant2}")
    private String tenant2DbName;

    @PostConstruct
    public void initializeTenantConnections() {
        // Simple initialization - all data goes to rms database
        // Tenant-specific databases will be created separately when needed
        log.info("Initialized database connection to: {}", dbName);
    }

    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        // Use simple connection factory pointing to rms database
        // Tenant data is stored in the same database for now
        return createTenantConnectionFactory(dbName);
    }

    private ConnectionFactory createTenantConnectionFactory(String databaseName) {
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

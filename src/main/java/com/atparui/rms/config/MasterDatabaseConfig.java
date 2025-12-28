package com.atparui.rms.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

@Configuration
public class MasterDatabaseConfig {

    @Value("${TENANT_DB_HOST:localhost}")
    private String tenantDbHost;

    @Value("${TENANT_DB_PORT:5432}")
    private int tenantDbPort;

    @Value("${TENANT_DB_NAME:rms}")
    private String tenantDbName;

    @Value("${TENANT_DB_USERNAME:rms}")
    private String tenantDbUsername;

    @Value("${TENANT_DB_PASSWORD:rms}")
    private String tenantDbPassword;

    @Bean("masterConnectionFactory")
    public ConnectionFactory masterConnectionFactory() {
        return new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(tenantDbHost)
                .port(tenantDbPort)
                .database(tenantDbName)
                .username(tenantDbUsername)
                .password(tenantDbPassword)
                .build()
        );
    }

    @Bean("masterR2dbcTemplate")
    public R2dbcEntityTemplate masterR2dbcTemplate() {
        return new R2dbcEntityTemplate(masterConnectionFactory());
    }

    @Bean("masterTransactionManager")
    public ReactiveTransactionManager masterTransactionManager(@Qualifier("masterConnectionFactory") ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}

package com.atparui.rms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the platform database.
 * This holds the default PostgreSQL admin credentials used to:
 * 1. Create tenant databases when user selects "Use Platform Database"
 * 2. Initialize template and default databases for each platform on startup
 *
 * Individual platforms can override these settings in the Platform entity
 * for per-platform PostgreSQL instances.
 */
@Configuration
@ConfigurationProperties(prefix = "platform.database")
public class PlatformDatabaseConfig {

    /**
     * PostgreSQL admin host (default: rms-postgresql for Docker, localhost for dev)
     */
    private String adminHost = "localhost";

    /**
     * PostgreSQL admin port
     */
    private Integer adminPort = 5432;

    /**
     * PostgreSQL superuser username (typically 'postgres')
     */
    private String adminUsername = "postgres";

    /**
     * PostgreSQL superuser password
     */
    private String adminPassword;

    /**
     * Default database for admin operations (typically 'postgres')
     */
    private String adminDatabase = "postgres";

    /**
     * Whether to initialize platform databases on startup
     */
    private boolean initializeOnStartup = true;

    /**
     * Whether to create demo data in default tenants
     */
    private boolean createDemoData = true;

    // Getters and Setters

    public String getAdminHost() {
        return adminHost;
    }

    public void setAdminHost(String adminHost) {
        this.adminHost = adminHost;
    }

    public Integer getAdminPort() {
        return adminPort;
    }

    public void setAdminPort(Integer adminPort) {
        this.adminPort = adminPort;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminDatabase() {
        return adminDatabase;
    }

    public void setAdminDatabase(String adminDatabase) {
        this.adminDatabase = adminDatabase;
    }

    public boolean isInitializeOnStartup() {
        return initializeOnStartup;
    }

    public void setInitializeOnStartup(boolean initializeOnStartup) {
        this.initializeOnStartup = initializeOnStartup;
    }

    public boolean isCreateDemoData() {
        return createDemoData;
    }

    public void setCreateDemoData(boolean createDemoData) {
        this.createDemoData = createDemoData;
    }

    /**
     * Get the JDBC URL for admin operations.
     * @return JDBC URL for connecting to the admin database
     */
    public String getAdminJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", adminHost, adminPort, adminDatabase);
    }

    /**
     * Get the JDBC URL for a specific database.
     * @param databaseName the database name
     * @return JDBC URL for connecting to the specified database
     */
    public String getJdbcUrl(String databaseName) {
        return String.format("jdbc:postgresql://%s:%d/%s", adminHost, adminPort, databaseName);
    }

    /**
     * Get the R2DBC URL for a specific database.
     * @param databaseName the database name
     * @return R2DBC URL for connecting to the specified database
     */
    public String getR2dbcUrl(String databaseName) {
        return String.format("r2dbc:postgresql://%s:%d/%s", adminHost, adminPort, databaseName);
    }

    @Override
    public String toString() {
        return (
            "PlatformDatabaseConfig{" +
            "adminHost='" +
            adminHost +
            '\'' +
            ", adminPort=" +
            adminPort +
            ", adminUsername='" +
            adminUsername +
            '\'' +
            ", adminDatabase='" +
            adminDatabase +
            '\'' +
            ", initializeOnStartup=" +
            initializeOnStartup +
            ", createDemoData=" +
            createDemoData +
            '}'
        );
    }
}

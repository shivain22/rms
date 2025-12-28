package com.atparui.rms.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatabaseProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseProvisioningService.class);

    @Value("${spring.r2dbc.url}")
    private String masterDbUrl;

    @Value("${spring.r2dbc.username}")
    private String masterDbUsername;

    @Value("${spring.r2dbc.password}")
    private String masterDbPassword;

    private String tenantPassword;
    private TenantLiquibaseService tenantLiquibaseService;

    public DatabaseProvisioningService(TenantLiquibaseService tenantLiquibaseService) {
        this.tenantLiquibaseService = tenantLiquibaseService;
    }

    public void createTenantDatabase(String tenantId) {
        createTenantDatabase(tenantId, false);
    }

    public void createTenantDatabase(String tenantId, boolean applyLiquibaseImmediately) {
        String dbName = "rms_" + tenantId;
        String dbUser = "rms_" + tenantId;
        this.tenantPassword = generatePassword();

        try {
            // Convert R2DBC URL to JDBC URL for admin operations
            String jdbcUrl = convertR2dbcToJdbc(masterDbUrl);

            try (Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUsername, masterDbPassword)) {
                // Create database
                createDatabase(connection, dbName);

                // Create user
                createUser(connection, dbUser, tenantPassword);

                // Grant privileges
                grantPrivileges(connection, dbName, dbUser);

                // Initialize schema
                if (applyLiquibaseImmediately) {
                    // Apply Liquibase changes from rms-service repository
                    String tenantJdbcUrl = convertR2dbcToJdbc(masterDbUrl).replace("/rms", "/" + dbName);
                    try {
                        tenantLiquibaseService.applyLiquibaseChanges(tenantId, tenantJdbcUrl, dbUser, tenantPassword);
                        log.info("Successfully applied Liquibase changes for tenant: {}", tenantId);
                    } catch (Exception e) {
                        log.error("Failed to apply Liquibase changes for tenant: {}, falling back to basic schema", tenantId, e);
                        // Fallback to basic schema if Liquibase fails
                        initializeTenantSchema(dbName, dbUser, tenantPassword);
                    }
                } else {
                    // Use basic schema initialization (Liquibase can be applied later)
                    initializeTenantSchema(dbName, dbUser, tenantPassword);
                }

                log.info("Successfully created database and user for tenant: {}", tenantId);
            }
        } catch (SQLException e) {
            log.error("Failed to create database for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to create tenant database", e);
        }
    }

    private void createDatabase(Connection connection, String dbName) throws SQLException {
        String sql = "CREATE DATABASE " + dbName + " WITH TEMPLATE template0 ENCODING='UTF8'";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            log.info("Created database: {}", dbName);
        }
    }

    private void createUser(Connection connection, String username, String password) throws SQLException {
        String sql = "CREATE USER " + username + " WITH PASSWORD '" + password + "'";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            log.info("Created user: {}", username);
        }
    }

    private void grantPrivileges(Connection connection, String dbName, String username) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Grant all privileges on database
            statement.executeUpdate("GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + username);

            // Connect to the new database to grant schema privileges
            String jdbcUrl = convertR2dbcToJdbc(masterDbUrl).replace("/rms", "/" + dbName);
            try (
                Connection dbConnection = DriverManager.getConnection(jdbcUrl, masterDbUsername, masterDbPassword);
                Statement dbStatement = dbConnection.createStatement()
            ) {
                dbStatement.executeUpdate("GRANT ALL ON SCHEMA public TO " + username);
                dbStatement.executeUpdate("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO " + username);
                dbStatement.executeUpdate("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO " + username);
                dbStatement.executeUpdate("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + username);
                dbStatement.executeUpdate("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO " + username);
            }

            log.info("Granted privileges to user: {} on database: {}", username, dbName);
        }
    }

    private void initializeTenantSchema(String dbName, String username, String password) {
        // This will run Liquibase on the new tenant database
        try {
            String jdbcUrl = convertR2dbcToJdbc(masterDbUrl).replace("/rms", "/" + dbName);

            // Here you would typically run Liquibase programmatically
            // For now, we'll create basic restaurant tables
            try (
                Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement()
            ) {
                // Create basic restaurant schema
                statement.executeUpdate(createRestaurantTablesSQL());

                log.info("Initialized schema for tenant database: {}", dbName);
            }
        } catch (SQLException e) {
            log.error("Failed to initialize schema for database: {}", dbName, e);
            throw new RuntimeException("Failed to initialize tenant schema", e);
        }
    }

    private String createRestaurantTablesSQL() {
        return """
        -- Restaurant branches
        CREATE TABLE branches (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            address TEXT,
            phone VARCHAR(20),
            email VARCHAR(100),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Menu categories
        CREATE TABLE menu_categories (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Menu items
        CREATE TABLE menu_items (
            id BIGSERIAL PRIMARY KEY,
            category_id BIGINT REFERENCES menu_categories(id),
            name VARCHAR(100) NOT NULL,
            description TEXT,
            price DECIMAL(10,2) NOT NULL,
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Orders
        CREATE TABLE orders (
            id BIGSERIAL PRIMARY KEY,
            branch_id BIGINT REFERENCES branches(id),
            order_number VARCHAR(50) UNIQUE NOT NULL,
            customer_name VARCHAR(100),
            customer_phone VARCHAR(20),
            total_amount DECIMAL(10,2) NOT NULL,
            status VARCHAR(20) DEFAULT 'PENDING',
            order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Order items
        CREATE TABLE order_items (
            id BIGSERIAL PRIMARY KEY,
            order_id BIGINT REFERENCES orders(id),
            menu_item_id BIGINT REFERENCES menu_items(id),
            quantity INTEGER NOT NULL,
            unit_price DECIMAL(10,2) NOT NULL,
            total_price DECIMAL(10,2) NOT NULL
        );
        """;
    }

    public void deleteTenantDatabase(String tenantId) {
        String dbName = "rms_" + tenantId;
        String dbUser = "rms_" + tenantId;

        try {
            String jdbcUrl = convertR2dbcToJdbc(masterDbUrl);

            try (
                Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUsername, masterDbPassword);
                Statement statement = connection.createStatement()
            ) {
                // Terminate connections to the database
                statement.executeUpdate("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + dbName + "'");

                // Drop database
                statement.executeUpdate("DROP DATABASE IF EXISTS " + dbName);

                // Drop user
                statement.executeUpdate("DROP USER IF EXISTS " + dbUser);

                log.info("Deleted database and user for tenant: {}", tenantId);
            }
        } catch (SQLException e) {
            log.error("Failed to delete database for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to delete tenant database", e);
        }
    }

    public String getTenantDatabaseUrl(String tenantId) {
        return masterDbUrl.replace("/rms", "/rms_" + tenantId);
    }

    public String getTenantDatabaseUsername(String tenantId) {
        return "rms_" + tenantId;
    }

    public String getTenantDatabasePassword(String tenantId) {
        return tenantPassword;
    }

    private String convertR2dbcToJdbc(String r2dbcUrl) {
        return r2dbcUrl.replace("r2dbc:postgresql://", "jdbc:postgresql://");
    }

    private String generatePassword() {
        return "pwd_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }
}

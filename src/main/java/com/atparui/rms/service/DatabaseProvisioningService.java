package com.atparui.rms.service;

import com.atparui.rms.config.PlatformDatabaseConfig;
import com.atparui.rms.domain.Platform;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
    private final TenantLiquibaseService tenantLiquibaseService;
    private final PlatformDatabaseConfig platformDatabaseConfig;

    public DatabaseProvisioningService(TenantLiquibaseService tenantLiquibaseService, PlatformDatabaseConfig platformDatabaseConfig) {
        this.tenantLiquibaseService = tenantLiquibaseService;
        this.platformDatabaseConfig = platformDatabaseConfig;
    }

    /**
     * Result object for database provisioning operations.
     */
    public static class ProvisioningResult {

        private final String databaseName;
        private final String username;
        private final String password;
        private final String host;
        private final Integer port;
        private final String jdbcUrl;
        private final String r2dbcUrl;
        private final String schemaName;

        public ProvisioningResult(String databaseName, String username, String password, String host, Integer port) {
            this(databaseName, username, password, host, port, "public");
        }

        public ProvisioningResult(String databaseName, String username, String password, String host, Integer port, String schemaName) {
            this.databaseName = databaseName;
            this.username = username;
            this.password = password;
            this.host = host;
            this.port = port;
            this.schemaName = schemaName != null ? schemaName : "public";
            this.jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            this.r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s", host, port, databaseName);
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public String getR2dbcUrl() {
            return r2dbcUrl;
        }

        public String getSchemaName() {
            return schemaName;
        }
    }

    public void createTenantDatabase(String tenantId) {
        createTenantDatabase(tenantId, false);
    }

    /**
     * Create a tenant database using platform database configuration.
     * This is used when user selects "Use Platform Database" option.
     *
     * @param platform the platform (can be null to use default config)
     * @param platformPrefix the platform prefix (e.g., "rms", "ecm")
     * @param tenantKey the tenant key
     * @param applyLiquibaseImmediately whether to apply Liquibase migrations immediately
     * @return ProvisioningResult with database connection details
     */
    public ProvisioningResult createTenantDatabaseForPlatform(
        Platform platform,
        String platformPrefix,
        String tenantKey,
        boolean applyLiquibaseImmediately
    ) {
        // Use platform-specific config or fall back to default
        String host = (platform != null && platform.getDatabaseHost() != null)
            ? platform.getDatabaseHost()
            : platformDatabaseConfig.getAdminHost();
        Integer port = (platform != null && platform.getDatabasePort() != null)
            ? platform.getDatabasePort()
            : platformDatabaseConfig.getAdminPort();
        String adminUser = (platform != null && platform.getDatabaseAdminUsername() != null)
            ? platform.getDatabaseAdminUsername()
            : platformDatabaseConfig.getAdminUsername();
        String adminPass = (platform != null && platform.getDatabaseAdminPassword() != null)
            ? platform.getDatabaseAdminPassword()
            : platformDatabaseConfig.getAdminPassword();

        // Database naming: {platform_prefix}_{tenant_key}
        String dbName = platformPrefix.toLowerCase() + "_" + tenantKey.toLowerCase().replace("-", "_");
        String dbUser = dbName; // User same as database name
        String dbPassword = generatePassword();

        log.info("Creating platform database: {} for tenant: {} on {}:{}", dbName, tenantKey, host, port);

        String adminJdbcUrl = String.format("jdbc:postgresql://%s:%d/postgres", host, port);

        boolean databaseCreated = false;
        boolean userCreated = false;

        try (Connection connection = DriverManager.getConnection(adminJdbcUrl, adminUser, adminPass)) {
            // Step 1: Create user if not exists
            try {
                if (!userExistsCheck(connection, dbUser)) {
                    createUser(connection, dbUser, dbPassword);
                    userCreated = true;
                    log.info("Created user: {}", dbUser);
                } else {
                    log.debug("User already exists: {}", dbUser);
                }
            } catch (SQLException e) {
                log.error("Failed to create user: {}", dbUser, e);
                throw new RuntimeException("Failed to create user: " + dbUser, e);
            }

            // Step 2: Create database
            try {
                if (!databaseExistsCheck(connection, dbName)) {
                    createDatabaseWithOwner(connection, dbName, dbUser);
                    databaseCreated = true;
                    log.info("Created database: {} with owner: {}", dbName, dbUser);
                } else {
                    log.debug("Database already exists: {}", dbName);
                }
            } catch (SQLException e) {
                log.error("Failed to create database: {}", dbName, e);
                rollbackDatabase(connection, dbName, dbUser, databaseCreated, userCreated);
                throw new RuntimeException("Failed to create database: " + dbName, e);
            }

            // Step 3: Grant privileges
            try {
                grantPrivilegesForPlatform(host, port, dbName, dbUser, adminUser, adminPass);
                log.info("Granted privileges to user: {} on database: {}", dbUser, dbName);
            } catch (SQLException e) {
                log.error("Failed to grant privileges to user: {} on database: {}", dbUser, dbName, e);
                rollbackDatabase(connection, dbName, dbUser, databaseCreated, userCreated);
                throw new RuntimeException("Failed to grant privileges", e);
            }

            // Step 4: Apply Liquibase if requested
            if (applyLiquibaseImmediately && databaseCreated) {
                String tenantJdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
                try {
                    tenantLiquibaseService.applyLiquibaseChanges(tenantKey, tenantJdbcUrl, dbUser, dbPassword);
                    log.info("Applied Liquibase changes for tenant: {}", tenantKey);
                } catch (Exception e) {
                    log.warn("Failed to apply Liquibase changes for tenant: {}, continuing with basic schema", tenantKey, e);
                }
            }

            log.info("Successfully created platform database: {} for tenant: {}", dbName, tenantKey);
            return new ProvisioningResult(dbName, dbUser, dbPassword, host, port);
        } catch (SQLException e) {
            log.error("Failed to create platform database for tenant: {}", tenantKey, e);
            throw new RuntimeException("Failed to create tenant database", e);
        }
    }

    /**
     * Create a database on an external server using user-provided admin credentials.
     * This is used for BYOD_CREATE mode when user wants us to create the database on their server.
     *
     * @param host the database server host
     * @param port the database server port
     * @param adminUsername admin user with CREATE DATABASE privilege
     * @param adminPassword admin user's password
     * @param databaseName name of the database to create
     * @param databaseUsername new user to create for this database
     * @param databasePassword password for the new database user
     * @param schemaName schema name (defaults to 'public')
     * @param vendorCode database vendor code (e.g., 'POSTGRESQL', 'MYSQL')
     * @return ProvisioningResult with database connection details
     */
    public ProvisioningResult createDatabaseOnExternalServer(
        String host,
        Integer port,
        String adminUsername,
        String adminPassword,
        String databaseName,
        String databaseUsername,
        String databasePassword,
        String schemaName,
        String vendorCode
    ) {
        log.info("Creating BYOD database: {} on external server {}:{}", databaseName, host, port);

        // Currently only PostgreSQL is supported for BYOD_CREATE
        if (vendorCode != null && !"POSTGRESQL".equalsIgnoreCase(vendorCode)) {
            throw new RuntimeException("BYOD_CREATE is currently only supported for PostgreSQL. Vendor: " + vendorCode);
        }

        String adminJdbcUrl = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
        String finalSchemaName = (schemaName != null && !schemaName.isEmpty()) ? schemaName : "public";

        boolean databaseCreated = false;
        boolean userCreated = false;

        try (Connection connection = DriverManager.getConnection(adminJdbcUrl, adminUsername, adminPassword)) {
            // Step 1: Create user if not exists
            try {
                if (!userExistsCheck(connection, databaseUsername)) {
                    createUser(connection, databaseUsername, databasePassword);
                    userCreated = true;
                    log.info("Created user: {} on external server", databaseUsername);
                } else {
                    // Update password for existing user
                    try (Statement stmt = connection.createStatement()) {
                        stmt.executeUpdate("ALTER USER " + databaseUsername + " WITH PASSWORD '" + databasePassword + "'");
                    }
                    log.debug("User already exists, updated password: {}", databaseUsername);
                }
            } catch (SQLException e) {
                log.error("Failed to create user on external server: {}", databaseUsername, e);
                throw new RuntimeException("Failed to create user: " + databaseUsername + ". Error: " + e.getMessage(), e);
            }

            // Step 2: Create database
            try {
                if (!databaseExistsCheck(connection, databaseName)) {
                    createDatabaseWithOwner(connection, databaseName, databaseUsername);
                    databaseCreated = true;
                    log.info("Created database: {} with owner: {} on external server", databaseName, databaseUsername);
                } else {
                    log.debug("Database already exists on external server: {}", databaseName);
                }
            } catch (SQLException e) {
                log.error("Failed to create database on external server: {}", databaseName, e);
                if (userCreated) {
                    try {
                        dropUserIfExists(connection, databaseUsername);
                    } catch (SQLException ex) {
                        log.warn("Failed to rollback user creation: {}", databaseUsername, ex);
                    }
                }
                throw new RuntimeException("Failed to create database: " + databaseName + ". Error: " + e.getMessage(), e);
            }

            // Step 3: Grant privileges
            try {
                grantPrivilegesForPlatform(host, port, databaseName, databaseUsername, adminUsername, adminPassword);
                log.info("Granted privileges to user: {} on database: {} on external server", databaseUsername, databaseName);
            } catch (SQLException e) {
                log.error("Failed to grant privileges on external server", e);
                rollbackDatabase(connection, databaseName, databaseUsername, databaseCreated, userCreated);
                throw new RuntimeException("Failed to grant privileges. Error: " + e.getMessage(), e);
            }

            // Step 4: Apply Liquibase migrations
            if (databaseCreated) {
                String tenantJdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
                try {
                    tenantLiquibaseService.applyLiquibaseChanges(databaseName, tenantJdbcUrl, databaseUsername, databasePassword);
                    log.info("Applied Liquibase changes for BYOD database: {}", databaseName);
                } catch (Exception e) {
                    log.warn("Failed to apply Liquibase changes for BYOD database: {}, continuing with basic schema", databaseName, e);
                }
            }

            log.info("Successfully created BYOD database: {} on external server {}:{}", databaseName, host, port);
            return new ProvisioningResult(databaseName, databaseUsername, databasePassword, host, port, finalSchemaName);
        } catch (SQLException e) {
            log.error("Failed to connect to external server for BYOD database creation: {}:{}", host, port, e);
            throw new RuntimeException("Failed to connect to external database server: " + e.getMessage(), e);
        }
    }

    private boolean databaseExistsCheck(Connection connection, String dbName) throws SQLException {
        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")
        ) {
            return rs.next();
        }
    }

    private boolean userExistsCheck(Connection connection, String username) throws SQLException {
        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_user WHERE usename = '" + username + "'")
        ) {
            return rs.next();
        }
    }

    private void createDatabaseWithOwner(Connection connection, String dbName, String owner) throws SQLException {
        String sql = "CREATE DATABASE " + dbName + " WITH OWNER = " + owner + " ENCODING = 'UTF8'";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private void grantPrivilegesForPlatform(String host, Integer port, String dbName, String username, String adminUser, String adminPass)
        throws SQLException {
        String dbJdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
        try (Connection dbConn = DriverManager.getConnection(dbJdbcUrl, adminUser, adminPass); Statement stmt = dbConn.createStatement()) {
            stmt.executeUpdate("GRANT ALL ON SCHEMA public TO " + username);
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO " + username);
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO " + username);
            stmt.executeUpdate("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + username);
            stmt.executeUpdate("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO " + username);
        }
    }

    /**
     * Delete a tenant database created on the platform.
     *
     * @param platform the platform (can be null to use default config)
     * @param dbName the database name
     * @param dbUser the database user
     */
    public void deleteTenantDatabaseForPlatform(Platform platform, String dbName, String dbUser) {
        String host = (platform != null && platform.getDatabaseHost() != null)
            ? platform.getDatabaseHost()
            : platformDatabaseConfig.getAdminHost();
        Integer port = (platform != null && platform.getDatabasePort() != null)
            ? platform.getDatabasePort()
            : platformDatabaseConfig.getAdminPort();
        String adminUser = (platform != null && platform.getDatabaseAdminUsername() != null)
            ? platform.getDatabaseAdminUsername()
            : platformDatabaseConfig.getAdminUsername();
        String adminPass = (platform != null && platform.getDatabaseAdminPassword() != null)
            ? platform.getDatabaseAdminPassword()
            : platformDatabaseConfig.getAdminPassword();

        String adminJdbcUrl = String.format("jdbc:postgresql://%s:%d/postgres", host, port);

        log.info("Deleting platform database: {} and user: {}", dbName, dbUser);

        try (Connection connection = DriverManager.getConnection(adminJdbcUrl, adminUser, adminPass)) {
            // Terminate connections and drop database
            terminateConnections(connection, dbName);
            dropDatabaseIfExists(connection, dbName);
            dropUserIfExists(connection, dbUser);
            log.info("Successfully deleted database: {} and user: {}", dbName, dbUser);
        } catch (SQLException e) {
            log.error("Failed to delete platform database: {}", dbName, e);
            throw new RuntimeException("Failed to delete platform database", e);
        }
    }

    private void terminateConnections(Connection connection, String dbName) throws SQLException {
        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + dbName + "' AND pid <> pg_backend_pid()"
            )
        ) {
            int count = 0;
            while (rs.next()) count++;
            if (count > 0) log.debug("Terminated {} connections to {}", count, dbName);
        }
    }

    private void dropDatabaseIfExists(Connection connection, String dbName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
        }
    }

    private void dropUserIfExists(Connection connection, String dbUser) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP USER IF EXISTS " + dbUser);
        }
    }

    public void createTenantDatabase(String tenantId, boolean applyLiquibaseImmediately) {
        String dbName = "rms_" + tenantId;
        String dbUser = "rms_" + tenantId;
        this.tenantPassword = generatePassword();

        boolean databaseCreated = false;
        boolean userCreated = false;

        try {
            // Convert R2DBC URL to JDBC URL for admin operations
            String jdbcUrl = convertR2dbcToJdbc(masterDbUrl);

            try (Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUsername, masterDbPassword)) {
                // Step 1: Create database
                try {
                    createDatabase(connection, dbName);
                    databaseCreated = true;
                    log.info("Step 1: Created database: {}", dbName);
                } catch (SQLException e) {
                    log.error("Failed to create database: {}", dbName, e);
                    throw new RuntimeException("Failed to create database: " + dbName, e);
                }

                // Step 2: Create user
                try {
                    createUser(connection, dbUser, tenantPassword);
                    userCreated = true;
                    log.info("Step 2: Created user: {}", dbUser);
                } catch (SQLException e) {
                    log.error("Failed to create user: {}", dbUser, e);
                    // Rollback: drop database if user creation fails
                    rollbackDatabase(connection, dbName, dbUser, databaseCreated, userCreated);
                    throw new RuntimeException("Failed to create user: " + dbUser, e);
                }

                // Step 3: Grant privileges
                try {
                    grantPrivileges(connection, dbName, dbUser);
                    log.info("Step 3: Granted privileges to user: {} on database: {}", dbUser, dbName);
                } catch (SQLException e) {
                    log.error("Failed to grant privileges to user: {} on database: {}", dbUser, dbName, e);
                    // Rollback: drop database and user if privilege grant fails
                    rollbackDatabase(connection, dbName, dbUser, databaseCreated, userCreated);
                    throw new RuntimeException("Failed to grant privileges", e);
                }

                // Step 4: Initialize schema
                try {
                    if (applyLiquibaseImmediately) {
                        // Apply Liquibase changes from rms-service repository
                        // Liquibase requires JDBC URLs, so convert R2DBC to JDBC
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
                } catch (Exception e) {
                    log.error("Failed to initialize schema for tenant: {}", tenantId, e);
                    // Rollback: drop database and user if schema initialization fails
                    rollbackDatabase(connection, dbName, dbUser, databaseCreated, userCreated);
                    throw new RuntimeException("Failed to initialize schema", e);
                }

                log.info("Successfully created database and user for tenant: {}", tenantId);
            }
        } catch (SQLException e) {
            log.error("Failed to create database for tenant: {}", tenantId, e);
            // Final rollback attempt if we still have a connection
            try {
                String jdbcUrl = convertR2dbcToJdbc(masterDbUrl);
                try (Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUsername, masterDbPassword)) {
                    rollbackDatabase(connection, dbName, dbUser, databaseCreated, userCreated);
                }
            } catch (Exception rollbackException) {
                log.error("Failed to rollback database during error handling for tenant: {}", tenantId, rollbackException);
            }
            throw new RuntimeException("Failed to create tenant database", e);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions (they already have rollback logic)
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating database for tenant: {}", tenantId, e);
            // Rollback on any other exception
            try {
                String jdbcUrl = convertR2dbcToJdbc(masterDbUrl);
                try (Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUsername, masterDbPassword)) {
                    rollbackDatabase(connection, dbName, dbUser, databaseCreated, userCreated);
                }
            } catch (Exception rollbackException) {
                log.error("Failed to rollback database during error handling for tenant: {}", tenantId, rollbackException);
            }
            throw new RuntimeException("Failed to create tenant database", e);
        }
    }

    /**
     * Rollback database creation by dropping database and user if they were created.
     *
     * @param connection the database connection
     * @param dbName the database name
     * @param dbUser the database user name
     * @param databaseCreated whether the database was created
     * @param userCreated whether the user was created
     */
    private void rollbackDatabase(Connection connection, String dbName, String dbUser, boolean databaseCreated, boolean userCreated) {
        log.warn("Rolling back database creation for database: {} and user: {}", dbName, dbUser);

        try {
            // Terminate any active connections to the database before dropping
            if (databaseCreated) {
                try (Statement statement = connection.createStatement()) {
                    // Terminate connections to the database (excluding current connection)
                    try (
                        ResultSet rs = statement.executeQuery(
                            "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" +
                            dbName +
                            "' AND pid <> pg_backend_pid()"
                        )
                    ) {
                        // Consume the result set
                        int terminatedCount = 0;
                        while (rs.next()) {
                            terminatedCount++;
                        }
                        if (terminatedCount > 0) {
                            log.debug("Terminated {} active connections to database: {}", terminatedCount, dbName);
                        }
                    }
                } catch (SQLException e) {
                    log.warn("Failed to terminate connections to database: {}, continuing with drop", dbName, e);
                }

                // Drop database
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
                    log.info("Rollback: Dropped database: {}", dbName);
                } catch (SQLException e) {
                    log.error("Rollback: Failed to drop database: {}", dbName, e);
                    throw e; // Re-throw to ensure we know if database drop failed
                }
            }

            // Drop user (should be done after database is dropped)
            if (userCreated) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DROP USER IF EXISTS " + dbUser);
                    log.info("Rollback: Dropped user: {}", dbUser);
                } catch (SQLException e) {
                    log.error("Rollback: Failed to drop user: {}", dbUser, e);
                    // Don't re-throw for user deletion - it's less critical
                }
            }
        } catch (Exception e) {
            log.error("Error during database rollback for database: {} and user: {}", dbName, dbUser, e);
            throw new RuntimeException("Failed to rollback database creation", e);
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

        log.info("Starting deletion of database: {} and user: {} for tenant: {}", dbName, dbUser, tenantId);

        try {
            String jdbcUrl = convertR2dbcToJdbc(masterDbUrl);

            try (Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUsername, masterDbPassword)) {
                // Step 1: Check if database exists
                boolean databaseExists = false;
                try (
                    Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")
                ) {
                    databaseExists = rs.next();
                } catch (SQLException e) {
                    log.warn("Failed to check if database exists: {}", dbName, e);
                }

                if (!databaseExists) {
                    log.info("Database {} does not exist, skipping database deletion", dbName);
                } else {
                    // Step 2: Terminate all connections to the database (excluding current connection)
                    try (Statement statement = connection.createStatement()) {
                        try (
                            ResultSet rs = statement.executeQuery(
                                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" +
                                dbName +
                                "' AND pid <> pg_backend_pid()"
                            )
                        ) {
                            // Consume the result set
                            int terminatedCount = 0;
                            while (rs.next()) {
                                terminatedCount++;
                            }
                            if (terminatedCount > 0) {
                                log.info("Terminated {} active connections to database: {}", terminatedCount, dbName);
                            } else {
                                log.debug("No active connections to terminate for database: {}", dbName);
                            }
                        }
                    } catch (SQLException e) {
                        log.warn("Failed to terminate connections to database: {}, continuing with drop", dbName, e);
                        // Continue even if termination fails - database might not have active connections
                    }

                    // Step 3: Drop database
                    try (Statement statement = connection.createStatement()) {
                        int rowsAffected = statement.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
                        if (rowsAffected > 0 || databaseExists) {
                            log.info("Deleted database: {}", dbName);
                        } else {
                            log.warn("Database {} was not dropped (may not exist)", dbName);
                        }
                    } catch (SQLException e) {
                        log.error("Failed to drop database: {}", dbName, e);
                        throw new RuntimeException("Failed to drop database: " + dbName, e);
                    }
                }

                // Step 4: Check if user exists
                boolean userExists = false;
                try (
                    Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT 1 FROM pg_user WHERE usename = '" + dbUser + "'")
                ) {
                    userExists = rs.next();
                } catch (SQLException e) {
                    log.warn("Failed to check if user exists: {}", dbUser, e);
                }

                if (!userExists) {
                    log.info("User {} does not exist, skipping user deletion", dbUser);
                } else {
                    // Step 5: Drop user (must be done after database is dropped)
                    try (Statement statement = connection.createStatement()) {
                        // First, revoke any remaining privileges
                        try {
                            statement.executeUpdate("REVOKE ALL PRIVILEGES ON DATABASE " + dbName + " FROM " + dbUser);
                        } catch (SQLException e) {
                            // Ignore - database might already be dropped or user might not have privileges
                            log.debug("Could not revoke privileges (database may not exist): {}", e.getMessage());
                        }

                        int rowsAffected = statement.executeUpdate("DROP USER IF EXISTS " + dbUser);
                        if (rowsAffected > 0 || userExists) {
                            log.info("Deleted user: {}", dbUser);
                        } else {
                            log.warn("User {} was not dropped (may not exist)", dbUser);
                        }
                    } catch (SQLException e) {
                        log.error("Failed to drop user: {}", dbUser, e);
                        // Don't throw - user might not exist or might have been dropped already
                        // But log it as an error for visibility
                        log.error("User {} may still exist after deletion attempt", dbUser);
                    }
                }

                log.info("Successfully completed deletion process for tenant: {} (database: {}, user: {})", tenantId, dbName, dbUser);
            }
        } catch (SQLException e) {
            log.error("Failed to delete database for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to delete tenant database", e);
        } catch (Exception e) {
            log.error("Unexpected error deleting database for tenant: {}", tenantId, e);
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

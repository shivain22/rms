package com.atparui.rms.service;

import com.atparui.rms.config.PlatformDatabaseConfig;
import com.atparui.rms.domain.Platform;
import com.atparui.rms.domain.Tenant;
import com.atparui.rms.repository.PlatformRepository;
import com.atparui.rms.repository.TenantRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * ApplicationRunner that initializes platform databases on startup.
 * For each active platform, it creates:
 * 1. Template database ({prefix}_template) - for schema initialization, marked as isTemplate=true
 * 2. Default database ({prefix}_default) - for demo purposes with sample data
 *
 * This is idempotent - it checks if databases/tenants already exist before creating.
 */
@Component
@Order(100) // Run after Liquibase migrations
public class PlatformDatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformDatabaseInitializer.class);

    private final PlatformDatabaseConfig platformDatabaseConfig;
    private final PlatformRepository platformRepository;
    private final TenantRepository tenantRepository;

    public PlatformDatabaseInitializer(
        PlatformDatabaseConfig platformDatabaseConfig,
        PlatformRepository platformRepository,
        TenantRepository tenantRepository
    ) {
        this.platformDatabaseConfig = platformDatabaseConfig;
        this.platformRepository = platformRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!platformDatabaseConfig.isInitializeOnStartup()) {
            log.info("Platform database initialization is disabled");
            return;
        }

        log.info("Starting platform database initialization...");
        log.debug("Platform database config: {}", platformDatabaseConfig);

        // Verify admin connection first
        if (!verifyAdminConnection()) {
            log.error("Cannot connect to PostgreSQL admin. Platform database initialization skipped.");
            log.error("Please ensure PostgreSQL is running and PLATFORM_DB_ADMIN_PASSWORD is set correctly.");
            return;
        }

        // Get all active platforms and initialize their databases
        platformRepository
            .findByActiveTrue()
            .flatMap(this::initializePlatformDatabases)
            .doOnError(e -> log.error("Error during platform database initialization", e))
            .subscribe(
                platform -> log.info("Completed initialization for platform: {}", platform.getPrefix()),
                error -> log.error("Platform database initialization failed", error),
                () -> log.info("Platform database initialization completed")
            );
    }

    /**
     * Verify that we can connect to PostgreSQL with admin credentials.
     */
    private boolean verifyAdminConnection() {
        String adminPassword = platformDatabaseConfig.getAdminPassword();
        if (adminPassword == null || adminPassword.isEmpty()) {
            log.warn("Platform database admin password is not configured (PLATFORM_DB_ADMIN_PASSWORD)");
            log.warn("Platform database initialization will be skipped. Set the password to enable auto-initialization.");
            return false;
        }

        try (
            Connection connection = DriverManager.getConnection(
                platformDatabaseConfig.getAdminJdbcUrl(),
                platformDatabaseConfig.getAdminUsername(),
                adminPassword
            )
        ) {
            log.info(
                "Successfully connected to PostgreSQL admin database at {}:{}",
                platformDatabaseConfig.getAdminHost(),
                platformDatabaseConfig.getAdminPort()
            );
            return true;
        } catch (SQLException e) {
            log.error("Failed to connect to PostgreSQL admin database: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Initialize databases for a platform.
     * Creates template and default databases if they don't exist.
     */
    private Mono<Platform> initializePlatformDatabases(Platform platform) {
        if (Boolean.TRUE.equals(platform.getDatabaseInitialized())) {
            log.debug("Platform {} already initialized, skipping", platform.getPrefix());
            return Mono.just(platform);
        }

        String prefix = platform.getPrefix().toLowerCase();
        log.info("Initializing databases for platform: {} ({})", platform.getName(), prefix);

        // Get database connection details (use platform-specific or default)
        String dbHost = platform.getDatabaseHost() != null ? platform.getDatabaseHost() : platformDatabaseConfig.getAdminHost();
        Integer dbPort = platform.getDatabasePort() != null ? platform.getDatabasePort() : platformDatabaseConfig.getAdminPort();
        String adminUser = platform.getDatabaseAdminUsername() != null
            ? platform.getDatabaseAdminUsername()
            : platformDatabaseConfig.getAdminUsername();
        String adminPass = platform.getDatabaseAdminPassword() != null
            ? platform.getDatabaseAdminPassword()
            : platformDatabaseConfig.getAdminPassword();

        String adminJdbcUrl = String.format("jdbc:postgresql://%s:%d/postgres", dbHost, dbPort);

        try (Connection connection = DriverManager.getConnection(adminJdbcUrl, adminUser, adminPass)) {
            // Create template database and tenant
            String templateDbName = prefix + "_template";
            String templateUser = prefix + "_template";
            String templatePassword = generatePassword();

            if (!databaseExists(connection, templateDbName)) {
                createDatabaseWithUser(connection, templateDbName, templateUser, templatePassword, dbHost, dbPort, adminUser, adminPass);
                log.info("Created template database: {}", templateDbName);
            } else {
                log.debug("Template database already exists: {}", templateDbName);
            }

            // Create default database and tenant (with demo data)
            String defaultDbName = prefix + "_default";
            String defaultUser = prefix + "_default";
            String defaultPassword = generatePassword();

            if (!databaseExists(connection, defaultDbName)) {
                createDatabaseWithUser(connection, defaultDbName, defaultUser, defaultPassword, dbHost, dbPort, adminUser, adminPass);
                log.info("Created default database: {}", defaultDbName);

                // Create demo data if enabled
                if (platformDatabaseConfig.isCreateDemoData()) {
                    createDemoData(defaultDbName, defaultUser, defaultPassword, dbHost, dbPort, prefix);
                }
            } else {
                log.debug("Default database already exists: {}", defaultDbName);
            }

            // Create tenant records if they don't exist
            return createTenantRecordsIfNotExist(
                platform,
                templateDbName,
                templateUser,
                templatePassword,
                defaultDbName,
                defaultUser,
                defaultPassword,
                dbHost,
                dbPort
            ).flatMap(p -> {
                // Mark platform as initialized
                p.setDatabaseInitialized(true);
                p.setLastModifiedDate(Instant.now());
                return platformRepository.save(p);
            });
        } catch (SQLException e) {
            log.error("Failed to initialize databases for platform: {}", prefix, e);
            return Mono.just(platform);
        }
    }

    /**
     * Check if a database exists.
     */
    private boolean databaseExists(Connection connection, String dbName) throws SQLException {
        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")
        ) {
            return rs.next();
        }
    }

    /**
     * Check if a user exists.
     */
    private boolean userExists(Connection connection, String username) throws SQLException {
        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_user WHERE usename = '" + username + "'")
        ) {
            return rs.next();
        }
    }

    /**
     * Create a database with its own user and grant privileges.
     */
    private void createDatabaseWithUser(
        Connection adminConn,
        String dbName,
        String username,
        String password,
        String host,
        Integer port,
        String adminUser,
        String adminPass
    ) throws SQLException {
        try (Statement stmt = adminConn.createStatement()) {
            // Create user if not exists
            if (!userExists(adminConn, username)) {
                stmt.executeUpdate("CREATE USER " + username + " WITH PASSWORD '" + password + "'");
                log.debug("Created user: {}", username);
            }

            // Create database
            stmt.executeUpdate("CREATE DATABASE " + dbName + " WITH OWNER = " + username + " ENCODING = 'UTF8'");
            log.debug("Created database: {}", dbName);

            // Grant privileges
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + username);
        }

        // Connect to the new database to set up schema privileges
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
     * Create demo data in the default database.
     */
    private void createDemoData(String dbName, String username, String password, String host, Integer port, String platformPrefix) {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password); Statement stmt = conn.createStatement()) {
            // Create basic schema and demo data based on platform type
            String sql = getDemoDataSql(platformPrefix);
            if (sql != null && !sql.isEmpty()) {
                stmt.executeUpdate(sql);
                log.info("Created demo data for {}", dbName);
            }
        } catch (SQLException e) {
            log.warn("Failed to create demo data for {}: {}", dbName, e.getMessage());
        }
    }

    /**
     * Get demo data SQL based on platform type.
     */
    private String getDemoDataSql(String platformPrefix) {
        return switch (platformPrefix.toLowerCase()) {
            case "rms" -> getRmsDemoDataSql();
            case "ecm" -> getEcmDemoDataSql();
            case "nbk" -> getNbkDemoDataSql();
            case "awd" -> getAwdDemoDataSql();
            case "vpm" -> getVpmDemoDataSql();
            case "hms" -> getHmsDemoDataSql();
            case "ems" -> getEmsDemoDataSql();
            case "dms" -> getDmsDemoDataSql();
            case "eim" -> getEimDemoDataSql();
            default -> getGenericDemoDataSql();
        };
    }

    /**
     * RMS (Restaurant Management System) demo data.
     */
    private String getRmsDemoDataSql() {
        return """
        -- Restaurant branches
        CREATE TABLE IF NOT EXISTS branches (
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
        CREATE TABLE IF NOT EXISTS menu_categories (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            display_order INTEGER DEFAULT 0,
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Menu items
        CREATE TABLE IF NOT EXISTS menu_items (
            id BIGSERIAL PRIMARY KEY,
            category_id BIGINT REFERENCES menu_categories(id),
            name VARCHAR(100) NOT NULL,
            description TEXT,
            price DECIMAL(10,2) NOT NULL,
            image_url VARCHAR(500),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Orders
        CREATE TABLE IF NOT EXISTS orders (
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
        CREATE TABLE IF NOT EXISTS order_items (
            id BIGSERIAL PRIMARY KEY,
            order_id BIGINT REFERENCES orders(id),
            menu_item_id BIGINT REFERENCES menu_items(id),
            quantity INTEGER NOT NULL,
            unit_price DECIMAL(10,2) NOT NULL,
            total_price DECIMAL(10,2) NOT NULL
        );

        -- Insert demo data
        INSERT INTO branches (name, address, phone, email) VALUES
        ('Main Branch', '123 Main Street, Downtown', '+1-555-0101', 'main@demo.com'),
        ('North Branch', '456 North Avenue', '+1-555-0102', 'north@demo.com')
        ON CONFLICT DO NOTHING;

        INSERT INTO menu_categories (name, description, display_order) VALUES
        ('Appetizers', 'Start your meal right', 1),
        ('Main Course', 'Our signature dishes', 2),
        ('Beverages', 'Refreshing drinks', 3),
        ('Desserts', 'Sweet endings', 4)
        ON CONFLICT DO NOTHING;

        INSERT INTO menu_items (category_id, name, description, price) VALUES
        (1, 'Spring Rolls', 'Crispy vegetable spring rolls', 8.99),
        (1, 'Soup of the Day', 'Chef special soup', 6.99),
        (2, 'Grilled Chicken', 'Herb-marinated grilled chicken', 18.99),
        (2, 'Pasta Primavera', 'Fresh vegetables with pasta', 15.99),
        (3, 'Fresh Lemonade', 'Homemade lemonade', 4.99),
        (3, 'Iced Tea', 'Refreshing iced tea', 3.99),
        (4, 'Chocolate Cake', 'Rich chocolate layer cake', 7.99),
        (4, 'Ice Cream', 'Vanilla ice cream', 5.99)
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * ECM (E-commerce) demo data.
     */
    private String getEcmDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS product_categories (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            parent_id BIGINT,
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS products (
            id BIGSERIAL PRIMARY KEY,
            category_id BIGINT REFERENCES product_categories(id),
            name VARCHAR(200) NOT NULL,
            description TEXT,
            sku VARCHAR(50) UNIQUE,
            price DECIMAL(10,2) NOT NULL,
            stock_quantity INTEGER DEFAULT 0,
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        INSERT INTO product_categories (name, description) VALUES
        ('Electronics', 'Electronic devices and accessories'),
        ('Clothing', 'Fashion and apparel'),
        ('Home & Garden', 'Home decor and gardening')
        ON CONFLICT DO NOTHING;

        INSERT INTO products (category_id, name, sku, price, stock_quantity) VALUES
        (1, 'Wireless Headphones', 'ELEC-001', 79.99, 50),
        (1, 'Smart Watch', 'ELEC-002', 199.99, 30),
        (2, 'Cotton T-Shirt', 'CLTH-001', 24.99, 100),
        (3, 'Garden Tools Set', 'HOME-001', 49.99, 25)
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * NBK (Neo Banking) demo data.
     */
    private String getNbkDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS account_types (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            min_balance DECIMAL(15,2) DEFAULT 0,
            interest_rate DECIMAL(5,4) DEFAULT 0,
            active BOOLEAN DEFAULT true
        );

        CREATE TABLE IF NOT EXISTS accounts (
            id BIGSERIAL PRIMARY KEY,
            account_type_id BIGINT REFERENCES account_types(id),
            account_number VARCHAR(20) UNIQUE NOT NULL,
            holder_name VARCHAR(200) NOT NULL,
            balance DECIMAL(15,2) DEFAULT 0,
            status VARCHAR(20) DEFAULT 'ACTIVE',
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        INSERT INTO account_types (name, description, min_balance, interest_rate) VALUES
        ('Savings', 'Regular savings account', 100.00, 0.0250),
        ('Current', 'Business current account', 500.00, 0),
        ('Fixed Deposit', 'Fixed term deposit', 1000.00, 0.0650)
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * AWD (Farming) demo data.
     */
    private String getAwdDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS farms (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            location TEXT,
            total_area_acres DECIMAL(10,2),
            farming_type VARCHAR(50),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS crops (
            id BIGSERIAL PRIMARY KEY,
            farm_id BIGINT REFERENCES farms(id),
            name VARCHAR(100) NOT NULL,
            variety VARCHAR(100),
            planted_date DATE,
            expected_harvest_date DATE,
            area_acres DECIMAL(10,2),
            status VARCHAR(50) DEFAULT 'PLANTED'
        );

        INSERT INTO farms (name, location, total_area_acres, farming_type) VALUES
        ('Demo Farm 1', 'North Valley', 50.00, 'AWD'),
        ('Demo Farm 2', 'South Plains', 75.00, 'DRY')
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * VPM (Visitor Pass Management) demo data.
     */
    private String getVpmDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS locations (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            address TEXT,
            contact_phone VARCHAR(20),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS pass_types (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            validity_hours INTEGER DEFAULT 8,
            requires_approval BOOLEAN DEFAULT false,
            active BOOLEAN DEFAULT true
        );

        INSERT INTO locations (name, address) VALUES
        ('Main Office', '100 Corporate Drive'),
        ('Research Center', '200 Innovation Way')
        ON CONFLICT DO NOTHING;

        INSERT INTO pass_types (name, description, validity_hours, requires_approval) VALUES
        ('Visitor', 'General visitor pass', 8, false),
        ('Contractor', 'Contractor access pass', 24, true),
        ('VIP', 'VIP guest pass', 12, true)
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * HMS (Hospital Management) demo data.
     */
    private String getHmsDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS departments (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            floor_number INTEGER,
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS doctors (
            id BIGSERIAL PRIMARY KEY,
            department_id BIGINT REFERENCES departments(id),
            name VARCHAR(200) NOT NULL,
            specialization VARCHAR(100),
            license_number VARCHAR(50),
            phone VARCHAR(20),
            active BOOLEAN DEFAULT true
        );

        INSERT INTO departments (name, description, floor_number) VALUES
        ('Emergency', 'Emergency and trauma care', 1),
        ('Cardiology', 'Heart and cardiovascular care', 3),
        ('Pediatrics', 'Children healthcare', 2),
        ('Orthopedics', 'Bone and joint care', 4)
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * EMS (Event Management) demo data.
     */
    private String getEmsDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS venues (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            address TEXT,
            capacity INTEGER,
            amenities TEXT,
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS event_types (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            default_duration_hours INTEGER DEFAULT 4,
            active BOOLEAN DEFAULT true
        );

        INSERT INTO venues (name, address, capacity) VALUES
        ('Grand Ballroom', '500 Convention Center Blvd', 500),
        ('Conference Hall A', '500 Convention Center Blvd', 100),
        ('Outdoor Pavilion', '500 Convention Center Blvd', 300)
        ON CONFLICT DO NOTHING;

        INSERT INTO event_types (name, description, default_duration_hours) VALUES
        ('Wedding', 'Wedding ceremonies and receptions', 8),
        ('Corporate', 'Corporate meetings and conferences', 4),
        ('Concert', 'Music concerts and performances', 6),
        ('Exhibition', 'Trade shows and exhibitions', 12)
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * DMS (Dairy Management) demo data.
     */
    private String getDmsDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS cattle_breeds (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT,
            avg_milk_yield_liters DECIMAL(5,2),
            active BOOLEAN DEFAULT true
        );

        CREATE TABLE IF NOT EXISTS cattle (
            id BIGSERIAL PRIMARY KEY,
            breed_id BIGINT REFERENCES cattle_breeds(id),
            tag_number VARCHAR(50) UNIQUE NOT NULL,
            name VARCHAR(100),
            birth_date DATE,
            gender VARCHAR(10),
            status VARCHAR(50) DEFAULT 'ACTIVE',
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        INSERT INTO cattle_breeds (name, description, avg_milk_yield_liters) VALUES
        ('Holstein', 'High milk production breed', 28.00),
        ('Jersey', 'High butterfat content milk', 20.00),
        ('Gir', 'Indian breed, heat tolerant', 12.00)
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * EIM (Export Import Management) demo data.
     */
    private String getEimDemoDataSql() {
        return """
        -- Customers (Buyers/Importers)
        CREATE TABLE IF NOT EXISTS customers (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(200) NOT NULL,
            company_name VARCHAR(200),
            country VARCHAR(100),
            address TEXT,
            email VARCHAR(100),
            phone VARCHAR(20),
            gst_number VARCHAR(50),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Vendors/Suppliers
        CREATE TABLE IF NOT EXISTS vendors (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(200) NOT NULL,
            company_name VARCHAR(200),
            address TEXT,
            email VARCHAR(100),
            phone VARCHAR(20),
            gst_number VARCHAR(50),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Products/Items
        CREATE TABLE IF NOT EXISTS products (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(200) NOT NULL,
            description TEXT,
            hsn_code VARCHAR(50),
            unit VARCHAR(50),
            price DECIMAL(15,2),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Ports
        CREATE TABLE IF NOT EXISTS ports (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            code VARCHAR(20) UNIQUE,
            country VARCHAR(100),
            port_type VARCHAR(50),
            active BOOLEAN DEFAULT true,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Orders/Contracts
        CREATE TABLE IF NOT EXISTS orders (
            id BIGSERIAL PRIMARY KEY,
            order_number VARCHAR(50) UNIQUE NOT NULL,
            customer_id BIGINT REFERENCES customers(id),
            order_date DATE NOT NULL,
            delivery_date DATE,
            currency VARCHAR(10) DEFAULT 'USD',
            total_amount DECIMAL(15,2),
            status VARCHAR(50) DEFAULT 'PENDING',
            payment_terms VARCHAR(200),
            shipment_terms VARCHAR(200),
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Order Items
        CREATE TABLE IF NOT EXISTS order_items (
            id BIGSERIAL PRIMARY KEY,
            order_id BIGINT REFERENCES orders(id),
            product_id BIGINT REFERENCES products(id),
            quantity DECIMAL(10,2) NOT NULL,
            unit_price DECIMAL(15,2) NOT NULL,
            total_price DECIMAL(15,2) NOT NULL,
            packaging_details TEXT,
            quality_specifications TEXT
        );

        -- Shipments
        CREATE TABLE IF NOT EXISTS shipments (
            id BIGSERIAL PRIMARY KEY,
            shipment_number VARCHAR(50) UNIQUE NOT NULL,
            order_id BIGINT REFERENCES orders(id),
            port_id BIGINT REFERENCES ports(id),
            container_number VARCHAR(50),
            bl_number VARCHAR(50),
            shipment_date DATE,
            status VARCHAR(50) DEFAULT 'PLANNED',
            cfs_reached BOOLEAN DEFAULT false,
            container_booked BOOLEAN DEFAULT false,
            port_stuffing_status VARCHAR(50),
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Expenses
        CREATE TABLE IF NOT EXISTS expenses (
            id BIGSERIAL PRIMARY KEY,
            shipment_id BIGINT REFERENCES shipments(id),
            expense_type VARCHAR(100) NOT NULL,
            description TEXT,
            amount DECIMAL(15,2) NOT NULL,
            currency VARCHAR(10) DEFAULT 'USD',
            expense_date DATE,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        -- Insert demo data
        INSERT INTO customers (name, company_name, country, email, phone) VALUES
        ('Global Trading Inc.', 'Global Trading Inc.', 'USA', 'contact@globaltrading.com', '+1-555-0101'),
        ('European Imports Ltd.', 'European Imports Ltd.', 'Germany', 'info@euimports.de', '+49-555-0102'),
        ('Asia Pacific Trading', 'Asia Pacific Trading', 'Singapore', 'sales@aptrading.sg', '+65-555-0103')
        ON CONFLICT DO NOTHING;

        INSERT INTO vendors (name, company_name, email, phone) VALUES
        ('Local Supplier Co.', 'Local Supplier Co.', 'supplier@local.com', '+91-555-0201'),
        ('Quality Materials Ltd.', 'Quality Materials Ltd.', 'info@qualitymaterials.com', '+91-555-0202')
        ON CONFLICT DO NOTHING;

        INSERT INTO products (name, description, hsn_code, unit, price) VALUES
        ('Cotton Yarn', 'Premium quality cotton yarn', '5205', 'KG', 5.50),
        ('Spices Mix', 'Assorted spices blend', '0904', 'KG', 12.00),
        ('Ceramic Tiles', 'Premium ceramic floor tiles', '6907', 'SQM', 8.50),
        ('Textile Fabric', '100% cotton fabric', '5208', 'MTR', 3.25)
        ON CONFLICT DO NOTHING;

        INSERT INTO ports (name, code, country, port_type) VALUES
        ('Mumbai Port', 'INMUM', 'India', 'SEA'),
        ('Nhava Sheva Port', 'INNSA', 'India', 'SEA'),
        ('Mundra Port', 'INMUN', 'India', 'SEA'),
        ('Los Angeles Port', 'USLAX', 'USA', 'SEA'),
        ('Hamburg Port', 'DEHAM', 'Germany', 'SEA')
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * Generic demo data for unknown platforms.
     */
    private String getGenericDemoDataSql() {
        return """
        CREATE TABLE IF NOT EXISTS settings (
            id BIGSERIAL PRIMARY KEY,
            key VARCHAR(100) UNIQUE NOT NULL,
            value TEXT,
            created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        INSERT INTO settings (key, value) VALUES
        ('app.name', 'Demo Application'),
        ('app.version', '1.0.0')
        ON CONFLICT DO NOTHING;
        """;
    }

    /**
     * Create tenant records in the database if they don't exist.
     */
    private Mono<Platform> createTenantRecordsIfNotExist(
        Platform platform,
        String templateDbName,
        String templateUser,
        String templatePassword,
        String defaultDbName,
        String defaultUser,
        String defaultPassword,
        String host,
        Integer port
    ) {
        String prefix = platform.getPrefix().toLowerCase();
        String templateTenantKey = prefix + "-template";
        String defaultTenantKey = prefix + "-default";

        // Check and create template tenant
        return tenantRepository
            .findByTenantKey(templateTenantKey)
            .switchIfEmpty(
                Mono.defer(() -> {
                    Tenant templateTenant = createTenantEntity(
                        platform,
                        templateTenantKey,
                        prefix + " Template",
                        templateDbName,
                        templateUser,
                        templatePassword,
                        host,
                        port,
                        true
                    );
                    return tenantRepository
                        .save(templateTenant)
                        .doOnSuccess(t -> log.info("Created template tenant: {}", templateTenantKey));
                })
            )
            .then(
                tenantRepository
                    .findByTenantKey(defaultTenantKey)
                    .switchIfEmpty(
                        Mono.defer(() -> {
                            Tenant defaultTenant = createTenantEntity(
                                platform,
                                defaultTenantKey,
                                prefix.toUpperCase() + " Default Demo",
                                defaultDbName,
                                defaultUser,
                                defaultPassword,
                                host,
                                port,
                                false
                            );
                            return tenantRepository
                                .save(defaultTenant)
                                .doOnSuccess(t -> log.info("Created default tenant: {}", defaultTenantKey));
                        })
                    )
            )
            .thenReturn(platform);
    }

    /**
     * Create a tenant entity with the given parameters.
     */
    private Tenant createTenantEntity(
        Platform platform,
        String tenantKey,
        String name,
        String dbName,
        String dbUser,
        String dbPassword,
        String host,
        Integer port,
        boolean isTemplate
    ) {
        Tenant tenant = new Tenant();
        tenant.setTenantKey(tenantKey);
        tenant.setTenantId(tenantKey);
        tenant.setName(name);
        tenant.setSubdomain(tenantKey + ".atparui.com");
        tenant.setPlatformId(platform.getId());
        tenant.setIsTemplate(isTemplate);
        tenant.setActive(true);

        // Database configuration
        tenant.setDatabaseOwnershipType("PLATFORM");
        tenant.setDatabaseProvisioningMode("AUTO_CREATE");
        tenant.setDatabaseVendorCode("POSTGRESQL");
        tenant.setDriverType("JDBC");
        tenant.setDatabaseHost(host);
        tenant.setDatabasePort(port);
        tenant.setDatabaseName(dbName);
        tenant.setDatabaseUsername(dbUser);
        tenant.setDatabasePassword(dbPassword);
        tenant.setDatabaseUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName));
        tenant.setSchemaName("public");

        // Keycloak configuration
        tenant.setRealmName(tenantKey + "-realm");
        tenant.setClientId(tenantKey + "-web");
        tenant.setClientSecret(generateClientSecret());
        tenant.setDefaultRoles("ROLE_ADMIN,ROLE_MANAGER,ROLE_USER,ROLE_ANONYMOUS");

        return tenant;
    }

    /**
     * Generate a random password.
     */
    private String generatePassword() {
        return "pwd_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }

    /**
     * Generate a client secret.
     */
    private String generateClientSecret() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}

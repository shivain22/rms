package com.atparui.rms.service;

import com.atparui.rms.domain.DatabaseVendor;
import com.atparui.rms.domain.DriverJar;
import com.atparui.rms.repository.DatabaseDriverRepository;
import com.atparui.rms.repository.DatabaseVendorRepository;
import com.atparui.rms.service.dto.DatabaseConnectionTestDTO;
import com.atparui.rms.service.dto.DatabaseConnectionTestResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DatabaseConnectionTestService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionTestService.class);

    private final DatabaseVendorRepository databaseVendorRepository;
    private final DatabaseDriverRepository databaseDriverRepository;
    private final DynamicDriverLoaderService dynamicDriverLoaderService;

    public DatabaseConnectionTestService(
        DatabaseVendorRepository databaseVendorRepository,
        DatabaseDriverRepository databaseDriverRepository,
        DynamicDriverLoaderService dynamicDriverLoaderService
    ) {
        this.databaseVendorRepository = databaseVendorRepository;
        this.databaseDriverRepository = databaseDriverRepository;
        this.dynamicDriverLoaderService = dynamicDriverLoaderService;
    }

    /**
     * Test database connection using provided credentials.
     *
     * @param testDTO the connection test DTO
     * @return Mono containing test result
     */
    public Mono<DatabaseConnectionTestResult> testConnection(DatabaseConnectionTestDTO testDTO) {
        return databaseVendorRepository
            .findByVendorCodeAndActiveTrue(testDTO.getVendorCode())
            .flatMap(vendor -> {
                // If driverId is specified, use that driver; otherwise use vendor default
                if (testDTO.getDriverId() != null) {
                    return databaseDriverRepository
                        .findById(testDTO.getDriverId())
                        .flatMap(driver -> {
                            try {
                                return Mono.just(testJdbcConnectionWithDriver(vendor, testDTO, driver));
                            } catch (Exception e) {
                                log.error("Error testing database connection with custom driver", e);
                                return Mono.just(
                                    new DatabaseConnectionTestResult(false, "Connection test failed: " + e.getMessage(), e.getMessage())
                                );
                            }
                        })
                        .switchIfEmpty(
                            Mono.just(new DatabaseConnectionTestResult(false, "Driver not found with ID: " + testDTO.getDriverId()))
                        );
                } else {
                    // Use default vendor driver
                    try {
                        return Mono.just(testJdbcConnection(vendor, testDTO));
                    } catch (Exception e) {
                        log.error("Error testing database connection", e);
                        return Mono.just(
                            new DatabaseConnectionTestResult(false, "Connection test failed: " + e.getMessage(), e.getMessage())
                        );
                    }
                }
            })
            .switchIfEmpty(
                Mono.just(new DatabaseConnectionTestResult(false, "Database vendor not found or inactive: " + testDTO.getVendorCode()))
            );
    }

    /**
     * Test JDBC connection using a specific uploaded driver.
     *
     * @param vendor the database vendor
     * @param testDTO the connection test DTO
     * @param driver the database driver to use
     * @return DatabaseConnectionTestResult
     */
    private DatabaseConnectionTestResult testJdbcConnectionWithDriver(
        DatabaseVendor vendor,
        DatabaseConnectionTestDTO testDTO,
        DriverJar driver
    ) throws Exception {
        long startTime = System.currentTimeMillis();
        java.sql.Connection connection = null;

        try {
            // Build JDBC URL from template
            String jdbcUrl = buildJdbcUrl(vendor, testDTO);
            log.debug("Testing connection to: {} using driver: {}", jdbcUrl.replace(testDTO.getPassword(), "***"), driver.getFileName());

            // Load driver dynamically
            java.sql.Driver driverInstance = dynamicDriverLoaderService.loadJdbcDriver(driver);

            // Create connection with timeout
            java.util.Properties props = new java.util.Properties();
            props.setProperty("user", testDTO.getUsername());
            props.setProperty("password", testDTO.getPassword());
            props.setProperty("connectTimeout", "5000"); // 5 second timeout

            connection = driverInstance.connect(jdbcUrl, props);
            if (connection == null) {
                // Some drivers don't accept URL, try DriverManager with loaded driver
                connection = java.sql.DriverManager.getConnection(jdbcUrl, props);
            }

            // Test connection with a simple query
            boolean isValid = connection.isValid(5); // 5 second timeout

            long connectionTime = System.currentTimeMillis() - startTime;

            if (isValid) {
                log.info(
                    "Database connection test successful for vendor: {} using driver: {} in {}ms",
                    vendor.getVendorCode(),
                    driver.getFileName(),
                    connectionTime
                );
                DatabaseConnectionTestResult result = new DatabaseConnectionTestResult(
                    true,
                    "Connection successful using " +
                    driver.getFileName() +
                    ". Connected to " +
                    vendor.getDisplayName() +
                    " in " +
                    connectionTime +
                    "ms"
                );
                result.setConnectionTimeMs(connectionTime);
                return result;
            } else {
                return new DatabaseConnectionTestResult(false, "Connection is not valid");
            }
        } catch (java.sql.SQLException e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            log.error(
                "Database connection test failed for vendor: {} using driver: {} after {}ms",
                vendor.getVendorCode(),
                driver.getFileName(),
                connectionTime,
                e
            );
            String errorMessage = "Connection failed: " + e.getMessage();
            if (e.getSQLState() != null) {
                errorMessage += " (SQL State: " + e.getSQLState() + ")";
            }
            DatabaseConnectionTestResult result = new DatabaseConnectionTestResult(false, errorMessage, e.getMessage());
            result.setConnectionTimeMs(connectionTime);
            return result;
        } catch (Exception e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during connection test with custom driver", e);
            DatabaseConnectionTestResult result = new DatabaseConnectionTestResult(
                false,
                "Unexpected error: " + e.getMessage(),
                e.getMessage()
            );
            result.setConnectionTimeMs(connectionTime);
            return result;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (java.sql.SQLException e) {
                    log.warn("Error closing test connection", e);
                }
            }
        }
    }

    /**
     * Test JDBC connection for the given vendor and connection details.
     *
     * @param vendor the database vendor
     * @param testDTO the connection test DTO
     * @return DatabaseConnectionTestResult
     */
    private DatabaseConnectionTestResult testJdbcConnection(DatabaseVendor vendor, DatabaseConnectionTestDTO testDTO) {
        long startTime = System.currentTimeMillis();
        Connection connection = null;

        try {
            // Build JDBC URL from template
            String jdbcUrl = buildJdbcUrl(vendor, testDTO);
            log.debug("Testing connection to: {}", jdbcUrl.replace(testDTO.getPassword(), "***"));

            // Load driver class if specified
            if (vendor.getDriverClassName() != null && !vendor.getDriverClassName().isEmpty()) {
                try {
                    Class.forName(vendor.getDriverClassName());
                } catch (ClassNotFoundException e) {
                    log.warn("Driver class not found: {}, attempting connection anyway", vendor.getDriverClassName());
                }
            }

            // Create connection with timeout
            Properties props = new Properties();
            props.setProperty("user", testDTO.getUsername());
            props.setProperty("password", testDTO.getPassword());
            props.setProperty("connectTimeout", "5000"); // 5 second timeout

            connection = DriverManager.getConnection(jdbcUrl, props);

            // Test connection with a simple query
            boolean isValid = connection.isValid(5); // 5 second timeout

            long connectionTime = System.currentTimeMillis() - startTime;

            if (isValid) {
                log.info("Database connection test successful for vendor: {} in {}ms", vendor.getVendorCode(), connectionTime);
                DatabaseConnectionTestResult result = new DatabaseConnectionTestResult(
                    true,
                    "Connection successful. Connected to " + vendor.getDisplayName() + " in " + connectionTime + "ms"
                );
                result.setConnectionTimeMs(connectionTime);
                return result;
            } else {
                return new DatabaseConnectionTestResult(false, "Connection is not valid");
            }
        } catch (SQLException e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            log.error("Database connection test failed for vendor: {} after {}ms", vendor.getVendorCode(), connectionTime, e);
            String errorMessage = "Connection failed: " + e.getMessage();
            if (e.getSQLState() != null) {
                errorMessage += " (SQL State: " + e.getSQLState() + ")";
            }
            DatabaseConnectionTestResult result = new DatabaseConnectionTestResult(false, errorMessage, e.getMessage());
            result.setConnectionTimeMs(connectionTime);
            return result;
        } catch (Exception e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during connection test", e);
            DatabaseConnectionTestResult result = new DatabaseConnectionTestResult(
                false,
                "Unexpected error: " + e.getMessage(),
                e.getMessage()
            );
            result.setConnectionTimeMs(connectionTime);
            return result;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.warn("Error closing test connection", e);
                }
            }
        }
    }

    /**
     * Build JDBC URL from vendor template and connection details.
     *
     * @param vendor the database vendor
     * @param testDTO the connection test DTO
     * @return JDBC URL string
     */
    private String buildJdbcUrl(DatabaseVendor vendor, DatabaseConnectionTestDTO testDTO) {
        String template = vendor.getJdbcUrlTemplate();
        if (template == null || template.isEmpty()) {
            // Fallback to default format
            template = "jdbc:" + vendor.getDriverKey() + "://{host}:{port}/{database}";
        }

        String url = template.replace("{host}", testDTO.getHost()).replace("{port}", String.valueOf(testDTO.getPort()));

        // Handle database name replacement
        if (url.contains("{database}")) {
            url = url.replace("{database}", testDTO.getDatabaseName());
        } else if (vendor.getVendorCode().equals("MSSQL")) {
            // MSSQL uses databaseName parameter
            url += ";databaseName=" + testDTO.getDatabaseName();
        }

        // Add schema if provided and vendor supports it
        if (testDTO.getSchemaName() != null && !testDTO.getSchemaName().isEmpty()) {
            if (vendor.getVendorCode().equals("POSTGRESQL")) {
                url += "?currentSchema=" + testDTO.getSchemaName();
            } else if (vendor.getVendorCode().equals("ORACLE")) {
                // Oracle uses schema as user, but we can set current_schema
                if (!url.contains("?")) {
                    url += "?current_schema=" + testDTO.getSchemaName();
                } else {
                    url += "&current_schema=" + testDTO.getSchemaName();
                }
            }
        }

        return url;
    }
}

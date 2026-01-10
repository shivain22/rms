# Database Drivers Setup Guide

This document describes how database drivers are managed and used in the RMS application.

## Overview

The RMS application uses a shared directory (`shared-drivers`) to store JDBC and R2DBC driver JAR files. This directory is accessible by both the RMS Gateway and RMS Service applications, allowing them to dynamically load database drivers at runtime.

## Directory Structure

The shared drivers directory is located at:

- **Local Development**: `C:\Users\shiva\eclipse-workspace\shared-drivers` (Windows) or `/path/to/shared-drivers` (Linux/Mac)
- **Docker**: `/shared-drivers` (mounted volume)

The directory structure follows this pattern:

```
shared-drivers/
├── postgresql/
│   ├── 16/
│   │   ├── jdbc/
│   │   └── r2dbc/
│   ├── 15/
│   └── 14/
├── mysql/
│   ├── 8.0/
│   └── 5.7/
├── oracle/
│   ├── 23c/
│   ├── 21c/
│   └── 19c/
├── mssql/
│   ├── 2022/
│   ├── 2019/
│   └── 2017/
└── db2/
    ├── 11.5/
    └── 11.1/
```

## Downloading Drivers

### Method 1: Maven Build (Recommended)

Drivers are automatically downloaded during Maven build using the `maven-dependency-plugin`. This happens during the `generate-resources` phase.

To download drivers:

```bash
mvn clean generate-resources
```

Or during a full build:

```bash
mvn clean install
```

To skip driver download (if you want to download manually):

```bash
mvn clean install -Dskip.driver.download=true
```

### Method 2: PowerShell Script

You can also use the PowerShell script to download drivers:

```powershell
powershell -ExecutionPolicy Bypass -File "C:\Users\shiva\eclipse-workspace\shared-drivers\download-drivers.ps1"
```

## Configuration

### Application Configuration

The storage path is configured in `application.yml`:

```yaml
database:
  driver:
    storage:
      path: ${DATABASE_DRIVER_STORAGE_PATH:C:\Users\shiva\eclipse-workspace\shared-drivers}
```

You can override this using the environment variable:

```bash
export DATABASE_DRIVER_STORAGE_PATH=/path/to/shared-drivers
```

### Docker Configuration

In Docker Compose, the shared-drivers directory is mounted as a volume:

```yaml
volumes:
  shared-drivers:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ${SHARED_DRIVERS_PATH:-C:/Users/shiva/eclipse-workspace/shared-drivers}
```

For Windows, use forward slashes or escaped backslashes in the path.

## Dynamic Driver Loading

The `DynamicDriverLoaderService` is responsible for loading drivers dynamically at runtime. It:

1. Loads JAR files from the shared directory using `DriverStorageService`
2. Creates URLClassLoader instances for each driver
3. Caches loaded drivers for performance
4. Supports both JDBC and R2DBC drivers

### Usage Example

```java
@Autowired
private DynamicDriverLoaderService driverLoaderService;

@Autowired
private DatabaseDriverRepository driverRepository;

public void testConnection(Long driverId) {
  driverRepository
    .findById(driverId)
    .flatMap(driver -> {
      try {
        java.sql.Driver jdbcDriver = driverLoaderService.loadJdbcDriver(driver);
        // Use the driver to create connections
        return Mono.just(jdbcDriver);
      } catch (Exception e) {
        return Mono.error(e);
      }
    });
}

```

## Database Connection Testing

The `DatabaseConnectionTestService` provides functionality to test database connections using dynamically loaded drivers:

```java
@Autowired
private DatabaseConnectionTestService connectionTestService;

public Mono<DatabaseConnectionTestResult> testConnection(DatabaseConnectionTestDTO testDTO) {
  return connectionTestService.testConnection(testDTO);
}

```

The service:

- Loads the appropriate driver from the shared directory
- Builds JDBC URL from vendor templates
- Tests the connection with timeout
- Returns detailed connection test results

## Schema Creation at Startup

Liquibase automatically runs at application startup to create and update the database schema. The configuration is in `LiquibaseConfiguration.java`:

- **Changelog**: `classpath:config/liquibase/master.xml`
- **Auto-execution**: Enabled by default (unless `no-liquibase` profile is active)
- **Default Data**: Imported from CSV files in `config/liquibase/data/`

The changelog includes:

1. Initial schema creation
2. Table creation for vendors, databases, versions, and driver_jars
3. Default data import (vendors, databases, versions, driver metadata)

## Docker Compose Setup

### Full Stack (Gateway + Service)

Use `docker-compose-full.yml` to run both gateway and service with shared drivers:

```bash
docker-compose -f docker-compose-full.yml up -d
```

### Local Development

Use `docker-compose-local.yml` for local development:

```bash
docker-compose -f docker-compose-local.yml up -d
```

### Environment Variables

Set the shared drivers path for Docker:

**Windows:**

```bash
set SHARED_DRIVERS_PATH=C:/Users/shiva/eclipse-workspace/shared-drivers
docker-compose up -d
```

**Linux/Mac:**

```bash
export SHARED_DRIVERS_PATH=/path/to/shared-drivers
docker-compose up -d
```

## Driver Versions

Current driver versions (as of setup):

| Database   | JDBC Version | R2DBC Version     |
| ---------- | ------------ | ----------------- |
| PostgreSQL | 42.7.3       | (Manual download) |
| MySQL      | 9.1.0        | 0.8.2.RELEASE     |
| Oracle     | 23.6.0.24.10 | 1.0.0             |
| SQL Server | 12.4.2.jre11 | 1.0.1.RELEASE     |
| DB2        | (Manual)     | (Manual)          |

## Troubleshooting

### Drivers Not Found

1. Check that the shared-drivers directory exists
2. Verify the path in `application.yml` or environment variable
3. Ensure drivers were downloaded (check directory contents)
4. Check file permissions (read access required)

### Connection Test Fails

1. Verify driver JAR file exists in shared directory
2. Check driver class name is correct
3. Verify JDBC URL template matches database vendor
4. Check network connectivity to database server
5. Review logs for detailed error messages

### Schema Not Created

1. Check Liquibase is enabled (not using `no-liquibase` profile)
2. Verify database connection is working
3. Check Liquibase logs for errors
4. Ensure changelog files are in classpath

## Maintenance

### Updating Drivers

1. Update versions in `pom.xml` (maven-dependency-plugin section)
2. Run `mvn clean generate-resources` to download new versions
3. Update CSV files in `config/liquibase/data/driver_jars.csv` with new file names
4. Restart application to use new drivers

### Adding New Database Vendors

1. Add vendor entry to `config/liquibase/data/databases.csv`
2. Add version entries to `config/liquibase/data/database_versions.csv`
3. Download driver JARs to appropriate directory
4. Add driver entries to `config/liquibase/data/driver_jars.csv`
5. The data will be imported automatically on next startup

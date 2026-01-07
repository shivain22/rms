# Database Configuration Hierarchy Refactoring

## Overview

This document describes the refactoring of the database configuration system to support a hierarchical structure: **DatabaseVendor → Database → DatabaseVersion → DriverJar**.

## Domain Model Structure

### Entity Hierarchy

```
DatabaseVendor (1) → (N) Database
Database (1) → (N) DatabaseVersion
DatabaseVersion (1) → (N) DriverJar
Tenant (N) → (1) DatabaseVersion
```

## Domain Entities

### 1. DatabaseVendor (Existing - Unchanged)

- **Table**: `database_vendors`
- **Purpose**: Represents database vendors (e.g., Oracle, MySQL, PostgreSQL)
- **Location**: `com.atparui.rms.domain.DatabaseVendor`

### 2. Database (New)

- **Table**: `databases`
- **Purpose**: Represents specific database products from a vendor
- **Example**: Oracle Database, MySQL, PostgreSQL (each vendor can have multiple databases)
- **Location**: `com.atparui.rms.domain.Database`
- **Key Fields**:
  - `vendorId` (FK to database_vendors)
  - `databaseCode` (unique code per vendor)
  - `displayName`
  - `defaultDriverClassName`
  - `defaultPort`
  - `jdbcUrlTemplate`
  - `r2dbcUrlTemplate`

### 3. DatabaseVersion (Refactored from DatabaseVendorVersion)

- **Table**: `database_versions` (renamed from `database_vendor_versions`)
- **Purpose**: Represents versions of a specific database
- **Location**: `com.atparui.rms.domain.DatabaseVersion`
- **Key Changes**:
  - `vendorId` → `databaseId` (now references Database instead of DatabaseVendor)
  - Table renamed from `database_vendor_versions` to `database_versions`

### 4. DriverJar (Refactored from DatabaseDriver)

- **Table**: `driver_jars` (renamed from `database_drivers`)
- **Purpose**: Stores JDBC/R2DBC driver JAR files for a specific database version
- **Location**: `com.atparui.rms.domain.DriverJar`
- **Key Changes**:
  - Removed `vendorId` (now only references DatabaseVersion)
  - `versionId` is now the only foreign key
  - Table renamed from `database_drivers` to `driver_jars`

### 5. Tenant (Updated)

- **Table**: `tenants`
- **Purpose**: Tenant configuration with reference to database version
- **Location**: `com.atparui.rms.domain.Tenant`
- **Key Changes**:
  - `databaseVendorVersionId` → `databaseVersionId`
  - `databaseDriverId` → `driverJarId`

## Database Migration

### Liquibase Changelog

- **File**: `20241203000008_refactor_database_hierarchy.xml`
- **Steps**:
  1. Create `databases` table
  2. Migrate existing vendor data to create default databases (one per vendor)
  3. Add `database_id` column to `database_vendor_versions`
  4. Populate `database_id` based on `vendor_id`
  5. Remove `vendor_id` from `database_vendor_versions`
  6. Rename `database_vendor_versions` to `database_versions`
  7. Remove `vendor_id` from `database_drivers`
  8. Rename `database_drivers` to `driver_jars`
  9. Update `tenants` table column names

## Files Created/Modified

### New Files

- `src/main/java/com/atparui/rms/domain/Database.java`
- `src/main/resources/config/liquibase/changelog/20241203000008_refactor_database_hierarchy.xml`

### Modified Files

- `src/main/java/com/atparui/rms/domain/DatabaseVendorVersion.java` → Renamed to `DatabaseVersion.java`
- `src/main/java/com/atparui/rms/domain/DatabaseDriver.java` → Renamed to `DriverJar.java`
- `src/main/java/com/atparui/rms/domain/Tenant.java` (updated field names)
- `src/main/resources/config/liquibase/master.xml` (added new changelog)

## Additional Updates Required

The following files reference the old entity names and will need to be updated:

### Repositories

- `DatabaseVendorVersionRepository` → Should be renamed to `DatabaseVersionRepository`
- `DatabaseDriverRepository` → Should be renamed to `DriverJarRepository`
- New repository needed: `DatabaseRepository`

### Services

- `DatabaseVendorVersionService` → Should be renamed to `DatabaseVersionService`
- `DatabaseDriverService` → Should be renamed to `DriverJarService`
- New service needed: `DatabaseService`

### REST Controllers

- `DatabaseVendorVersionResource` → Should be renamed to `DatabaseVersionResource`
- `DatabaseDriverResource` → Should be renamed to `DriverJarResource`
- New controller needed: `DatabaseResource`

### Other Services

- `DynamicDriverLoaderService` - Update references to `DatabaseDriver` → `DriverJar`
- `DatabaseConnectionTestService` - Update references to `DatabaseDriver` → `DriverJar`

## Usage Example

```java
// Hierarchy: Oracle (Vendor) → Oracle Database (Database) → 19c (Version) → ojdbc8.jar (DriverJar)

DatabaseVendor oracle = new DatabaseVendor("ORACLE", "Oracle", ...);
Database oracleDb = new Database(oracle.getId(), "ORACLE_DB", "Oracle Database", ...);
DatabaseVersion version19c = new DatabaseVersion(oracleDb.getId(), "19c", "Oracle Database 19c", ...);
DriverJar ojdbc8 = new DriverJar(version19c.getId(), "JDBC", "/drivers/ojdbc8.jar", "ojdbc8.jar");

Tenant tenant = new Tenant(...);
tenant.setDatabaseVersionId(version19c.getId());
tenant.setDriverJarId(ojdbc8.getId());
```

## Benefits

1. **Flexibility**: Supports vendors with multiple database products (e.g., Oracle has Oracle Database, Oracle MySQL)
2. **Clear Hierarchy**: Better organization of database configurations
3. **Scalability**: Easy to add new databases and versions
4. **Data Integrity**: Proper foreign key relationships ensure data consistency

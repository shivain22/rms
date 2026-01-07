package com.atparui.rms.service;

import com.atparui.rms.domain.Database;
import com.atparui.rms.domain.DatabaseVendor;
import com.atparui.rms.domain.DatabaseVersion;
import com.atparui.rms.repository.DatabaseRepository;
import com.atparui.rms.repository.DatabaseVendorRepository;
import com.atparui.rms.repository.DatabaseVendorVersionRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service to initialize database vendor, database, and version data with latest versions and proper URL templates.
 */
@Component
@Order(1)
public class DatabaseDataInitializationService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDataInitializationService.class);

    private final DatabaseVendorRepository databaseVendorRepository;
    private final DatabaseRepository databaseRepository;
    private final DatabaseVendorVersionRepository databaseVersionRepository;

    public DatabaseDataInitializationService(
        DatabaseVendorRepository databaseVendorRepository,
        DatabaseRepository databaseRepository,
        DatabaseVendorVersionRepository databaseVersionRepository
    ) {
        this.databaseVendorRepository = databaseVendorRepository;
        this.databaseRepository = databaseRepository;
        this.databaseVersionRepository = databaseVersionRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Starting database data initialization...");
        initializeDatabaseVendors()
            .then(initializeDatabases())
            .then(initializeDatabaseVersions())
            .doOnSuccess(v -> log.info("Database data initialization completed successfully"))
            .doOnError(error -> log.error("Error during database data initialization", error))
            .subscribe();
    }

    private Mono<Void> initializeDatabaseVendors() {
        log.info("Initializing database vendors...");

        return Flux.just(createPostgreSQLVendor(), createMySQLVendor(), createOracleVendor(), createMSSQLVendor(), createDB2Vendor())
            .flatMap(vendor ->
                databaseVendorRepository
                    .existsByVendorCode(vendor.getVendorCode())
                    .flatMap(exists -> {
                        if (exists) {
                            // Update existing vendor
                            return databaseVendorRepository
                                .findByVendorCode(vendor.getVendorCode())
                                .flatMap(existing -> {
                                    existing.setDisplayName(vendor.getDisplayName());
                                    existing.setDefaultPort(vendor.getDefaultPort());
                                    existing.setDriverKey(vendor.getDriverKey());
                                    existing.setDescription(vendor.getDescription());
                                    existing.setJdbcUrlTemplate(vendor.getJdbcUrlTemplate());
                                    existing.setR2dbcUrlTemplate(vendor.getR2dbcUrlTemplate());
                                    existing.setDriverClassName(vendor.getDriverClassName());
                                    existing.setActive(vendor.getActive());
                                    log.info("Updating vendor: {}", vendor.getVendorCode());
                                    return databaseVendorRepository.save(existing);
                                });
                        } else {
                            // Create new vendor
                            log.info("Creating vendor: {}", vendor.getVendorCode());
                            return databaseVendorRepository.save(vendor);
                        }
                    })
            )
            .then();
    }

    private Mono<Void> initializeDatabases() {
        log.info("Initializing databases...");

        return databaseVendorRepository
            .findAllActive()
            .collectMap(DatabaseVendor::getVendorCode, vendor -> vendor)
            .flatMapMany(vendorMap -> {
                return Flux.just(
                    createPostgreSQLDatabase(vendorMap.get("POSTGRESQL")),
                    createMySQLDatabase(vendorMap.get("MYSQL")),
                    createOracleDatabase(vendorMap.get("ORACLE")),
                    createMSSQLDatabase(vendorMap.get("MSSQL")),
                    createDB2Database(vendorMap.get("DB2"))
                ).filter(database -> database != null);
            })
            .flatMap(database ->
                databaseRepository
                    .existsByVendorIdAndDatabaseCode(database.getVendorId(), database.getDatabaseCode())
                    .flatMap(exists -> {
                        if (exists) {
                            // Update existing database
                            return databaseRepository
                                .findByVendorId(database.getVendorId())
                                .filter(db -> db.getDatabaseCode().equals(database.getDatabaseCode()))
                                .next()
                                .flatMap(existing -> {
                                    existing.setDisplayName(database.getDisplayName());
                                    existing.setDescription(database.getDescription());
                                    existing.setDefaultDriverClassName(database.getDefaultDriverClassName());
                                    existing.setDefaultPort(database.getDefaultPort());
                                    existing.setJdbcUrlTemplate(database.getJdbcUrlTemplate());
                                    existing.setR2dbcUrlTemplate(database.getR2dbcUrlTemplate());
                                    existing.setActive(database.getActive());
                                    log.info("Updating database: {}", database.getDatabaseCode());
                                    return databaseRepository.save(existing);
                                });
                        } else {
                            // Create new database
                            log.info("Creating database: {}", database.getDatabaseCode());
                            return databaseRepository.save(database);
                        }
                    })
            )
            .then();
    }

    private Mono<Void> initializeDatabaseVersions() {
        log.info("Initializing database versions...");

        return databaseRepository
            .findAllActive()
            .collectMap(Database::getDatabaseCode, database -> database)
            .flatMapMany(databaseMap -> {
                return Flux.just(
                    // PostgreSQL versions
                    createPostgreSQLVersion(databaseMap.get("POSTGRESQL"), "17", "PostgreSQL 17", LocalDate.of(2024, 9, 12), true, true),
                    createPostgreSQLVersion(
                        databaseMap.get("POSTGRESQL"),
                        "16",
                        "PostgreSQL 16",
                        LocalDate.of(2023, 9, 14),
                        LocalDate.of(2026, 11, 12),
                        true,
                        false
                    ),
                    createPostgreSQLVersion(
                        databaseMap.get("POSTGRESQL"),
                        "15",
                        "PostgreSQL 15",
                        LocalDate.of(2022, 10, 13),
                        LocalDate.of(2027, 11, 11),
                        true,
                        false
                    ),
                    // MySQL versions
                    createMySQLVersion(databaseMap.get("MYSQL"), "9.0", "MySQL 9.0", LocalDate.of(2024, 10, 21), true, true),
                    createMySQLVersion(databaseMap.get("MYSQL"), "8.4", "MySQL 8.4", LocalDate.of(2024, 4, 18), true, false),
                    createMySQLVersion(
                        databaseMap.get("MYSQL"),
                        "8.0",
                        "MySQL 8.0",
                        LocalDate.of(2018, 4, 19),
                        LocalDate.of(2026, 4, 30),
                        true,
                        false
                    ),
                    // Oracle versions
                    createOracleVersion(databaseMap.get("ORACLE"), "23c", "Oracle Database 23c", LocalDate.of(2023, 4, 4), true, true),
                    createOracleVersion(
                        databaseMap.get("ORACLE"),
                        "21c",
                        "Oracle Database 21c",
                        LocalDate.of(2020, 12, 8),
                        LocalDate.of(2024, 4, 30),
                        true,
                        false
                    ),
                    createOracleVersion(
                        databaseMap.get("ORACLE"),
                        "19c",
                        "Oracle Database 19c",
                        LocalDate.of(2019, 2, 13),
                        LocalDate.of(2026, 4, 30),
                        true,
                        false
                    ),
                    // SQL Server versions
                    createMSSQLVersion(databaseMap.get("MSSQL"), "2022", "SQL Server 2022", LocalDate.of(2022, 11, 16), true, true),
                    createMSSQLVersion(
                        databaseMap.get("MSSQL"),
                        "2019",
                        "SQL Server 2019",
                        LocalDate.of(2019, 11, 4),
                        LocalDate.of(2025, 1, 7),
                        true,
                        false
                    ),
                    createMSSQLVersion(
                        databaseMap.get("MSSQL"),
                        "2017",
                        "SQL Server 2017",
                        LocalDate.of(2017, 10, 2),
                        LocalDate.of(2024, 10, 8),
                        false,
                        false
                    ),
                    // DB2 versions
                    createDB2Version(databaseMap.get("DB2"), "11.5.9", "DB2 11.5.9", LocalDate.of(2024, 6, 28), true, true),
                    createDB2Version(databaseMap.get("DB2"), "11.5.8", "DB2 11.5.8", LocalDate.of(2023, 12, 15), true, false),
                    createDB2Version(databaseMap.get("DB2"), "11.5.7", "DB2 11.5.7", LocalDate.of(2023, 6, 30), true, false)
                ).filter(version -> version != null);
            })
            .flatMap(version ->
                databaseVersionRepository
                    .existsByDatabaseIdAndVersion(version.getDatabaseId(), version.getVersion())
                    .flatMap(exists -> {
                        if (exists) {
                            // Update existing version
                            return databaseVersionRepository
                                .findByDatabaseIdAndVersion(version.getDatabaseId(), version.getVersion())
                                .flatMap(existing -> {
                                    existing.setDisplayName(version.getDisplayName());
                                    existing.setReleaseDate(version.getReleaseDate());
                                    existing.setEndOfLifeDate(version.getEndOfLifeDate());
                                    existing.setReleaseNotes(version.getReleaseNotes());
                                    existing.setIsSupported(version.getIsSupported());
                                    existing.setIsRecommended(version.getIsRecommended());
                                    existing.setActive(version.getActive());
                                    log.info("Updating version: {} for database ID: {}", version.getVersion(), version.getDatabaseId());
                                    return databaseVersionRepository.save(existing);
                                });
                        } else {
                            // Create new version
                            log.info("Creating version: {} for database ID: {}", version.getVersion(), version.getDatabaseId());
                            return databaseVersionRepository.save(version);
                        }
                    })
            )
            .then();
    }

    // Vendor creation methods
    private DatabaseVendor createPostgreSQLVendor() {
        DatabaseVendor vendor = new DatabaseVendor();
        vendor.setVendorCode("POSTGRESQL");
        vendor.setDisplayName("PostgreSQL");
        vendor.setDefaultPort(5432);
        vendor.setDriverKey("postgresql");
        vendor.setDescription("PostgreSQL is a powerful, open source object-relational database system");
        vendor.setJdbcUrlTemplate("jdbc:postgresql://{host}:{port}/{database}");
        vendor.setR2dbcUrlTemplate("r2dbc:postgresql://{host}:{port}/{database}");
        vendor.setDriverClassName("org.postgresql.Driver");
        vendor.setActive(true);
        return vendor;
    }

    private DatabaseVendor createMySQLVendor() {
        DatabaseVendor vendor = new DatabaseVendor();
        vendor.setVendorCode("MYSQL");
        vendor.setDisplayName("MySQL");
        vendor.setDefaultPort(3306);
        vendor.setDriverKey("mysql");
        vendor.setDescription("MySQL is an open-source relational database management system");
        vendor.setJdbcUrlTemplate("jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=UTC");
        vendor.setR2dbcUrlTemplate("r2dbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=UTC");
        vendor.setDriverClassName("com.mysql.cj.jdbc.Driver");
        vendor.setActive(true);
        return vendor;
    }

    private DatabaseVendor createOracleVendor() {
        DatabaseVendor vendor = new DatabaseVendor();
        vendor.setVendorCode("ORACLE");
        vendor.setDisplayName("Oracle");
        vendor.setDefaultPort(1521);
        vendor.setDriverKey("oracle");
        vendor.setDescription("Oracle Database is a multi-model database management system");
        vendor.setJdbcUrlTemplate("jdbc:oracle:thin:@{host}:{port}:{database}");
        vendor.setR2dbcUrlTemplate("r2dbc:oracle://{host}:{port}/{database}");
        vendor.setDriverClassName("oracle.jdbc.OracleDriver");
        vendor.setActive(true);
        return vendor;
    }

    private DatabaseVendor createMSSQLVendor() {
        DatabaseVendor vendor = new DatabaseVendor();
        vendor.setVendorCode("MSSQL");
        vendor.setDisplayName("Microsoft SQL Server");
        vendor.setDefaultPort(1433);
        vendor.setDriverKey("mssql");
        vendor.setDescription("Microsoft SQL Server is a relational database management system");
        vendor.setJdbcUrlTemplate("jdbc:sqlserver://{host}:{port};databaseName={database};encrypt=true;trustServerCertificate=true");
        vendor.setR2dbcUrlTemplate("r2dbc:mssql://{host}:{port}/{database}");
        vendor.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        vendor.setActive(true);
        return vendor;
    }

    private DatabaseVendor createDB2Vendor() {
        DatabaseVendor vendor = new DatabaseVendor();
        vendor.setVendorCode("DB2");
        vendor.setDisplayName("IBM DB2");
        vendor.setDefaultPort(50000);
        vendor.setDriverKey("db2");
        vendor.setDescription("IBM DB2 is a family of data management products");
        vendor.setJdbcUrlTemplate("jdbc:db2://{host}:{port}/{database}");
        vendor.setR2dbcUrlTemplate("r2dbc:db2://{host}:{port}/{database}");
        vendor.setDriverClassName("com.ibm.db2.jcc.DB2Driver");
        vendor.setActive(true);
        return vendor;
    }

    // Database creation methods
    private Database createPostgreSQLDatabase(DatabaseVendor vendor) {
        if (vendor == null) return null;
        Database database = new Database();
        database.setVendorId(vendor.getId());
        database.setDatabaseCode("POSTGRESQL");
        database.setDisplayName("PostgreSQL");
        database.setDescription("PostgreSQL is a powerful, open source object-relational database system");
        database.setDefaultDriverClassName("org.postgresql.Driver");
        database.setDefaultPort(5432);
        database.setJdbcUrlTemplate("jdbc:postgresql://{host}:{port}/{database}");
        database.setR2dbcUrlTemplate("r2dbc:postgresql://{host}:{port}/{database}");
        database.setActive(true);
        return database;
    }

    private Database createMySQLDatabase(DatabaseVendor vendor) {
        if (vendor == null) return null;
        Database database = new Database();
        database.setVendorId(vendor.getId());
        database.setDatabaseCode("MYSQL");
        database.setDisplayName("MySQL");
        database.setDescription("MySQL is an open-source relational database management system");
        database.setDefaultDriverClassName("com.mysql.cj.jdbc.Driver");
        database.setDefaultPort(3306);
        database.setJdbcUrlTemplate("jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=UTC");
        database.setR2dbcUrlTemplate("r2dbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=UTC");
        database.setActive(true);
        return database;
    }

    private Database createOracleDatabase(DatabaseVendor vendor) {
        if (vendor == null) return null;
        Database database = new Database();
        database.setVendorId(vendor.getId());
        database.setDatabaseCode("ORACLE");
        database.setDisplayName("Oracle Database");
        database.setDescription("Oracle Database is a multi-model database management system");
        database.setDefaultDriverClassName("oracle.jdbc.OracleDriver");
        database.setDefaultPort(1521);
        database.setJdbcUrlTemplate("jdbc:oracle:thin:@{host}:{port}:{database}");
        database.setR2dbcUrlTemplate("r2dbc:oracle://{host}:{port}/{database}");
        database.setActive(true);
        return database;
    }

    private Database createMSSQLDatabase(DatabaseVendor vendor) {
        if (vendor == null) return null;
        Database database = new Database();
        database.setVendorId(vendor.getId());
        database.setDatabaseCode("MSSQL");
        database.setDisplayName("Microsoft SQL Server");
        database.setDescription("Microsoft SQL Server is a relational database management system");
        database.setDefaultDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        database.setDefaultPort(1433);
        database.setJdbcUrlTemplate("jdbc:sqlserver://{host}:{port};databaseName={database};encrypt=true;trustServerCertificate=true");
        database.setR2dbcUrlTemplate("r2dbc:mssql://{host}:{port}/{database}");
        database.setActive(true);
        return database;
    }

    private Database createDB2Database(DatabaseVendor vendor) {
        if (vendor == null) return null;
        Database database = new Database();
        database.setVendorId(vendor.getId());
        database.setDatabaseCode("DB2");
        database.setDisplayName("IBM DB2");
        database.setDescription("IBM DB2 is a family of data management products");
        database.setDefaultDriverClassName("com.ibm.db2.jcc.DB2Driver");
        database.setDefaultPort(50000);
        database.setJdbcUrlTemplate("jdbc:db2://{host}:{port}/{database}");
        database.setR2dbcUrlTemplate("r2dbc:db2://{host}:{port}/{database}");
        database.setActive(true);
        return database;
    }

    // Version creation methods
    private DatabaseVersion createPostgreSQLVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        return createPostgreSQLVersion(database, version, displayName, releaseDate, null, isSupported, isRecommended);
    }

    private DatabaseVersion createPostgreSQLVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        LocalDate endOfLifeDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        if (database == null) return null;
        DatabaseVersion dbVersion = new DatabaseVersion();
        dbVersion.setDatabaseId(database.getId());
        dbVersion.setVersion(version);
        dbVersion.setDisplayName(displayName);
        dbVersion.setReleaseDate(releaseDate);
        dbVersion.setEndOfLifeDate(endOfLifeDate);
        dbVersion.setReleaseNotes("PostgreSQL " + version + " release with enhanced features and performance improvements");
        dbVersion.setIsSupported(isSupported);
        dbVersion.setIsRecommended(isRecommended);
        dbVersion.setActive(true);
        return dbVersion;
    }

    private DatabaseVersion createMySQLVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        return createMySQLVersion(database, version, displayName, releaseDate, null, isSupported, isRecommended);
    }

    private DatabaseVersion createMySQLVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        LocalDate endOfLifeDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        if (database == null) return null;
        DatabaseVersion dbVersion = new DatabaseVersion();
        dbVersion.setDatabaseId(database.getId());
        dbVersion.setVersion(version);
        dbVersion.setDisplayName(displayName);
        dbVersion.setReleaseDate(releaseDate);
        dbVersion.setEndOfLifeDate(endOfLifeDate);
        dbVersion.setReleaseNotes("MySQL " + version + " release with new features and security updates");
        dbVersion.setIsSupported(isSupported);
        dbVersion.setIsRecommended(isRecommended);
        dbVersion.setActive(true);
        return dbVersion;
    }

    private DatabaseVersion createOracleVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        return createOracleVersion(database, version, displayName, releaseDate, null, isSupported, isRecommended);
    }

    private DatabaseVersion createOracleVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        LocalDate endOfLifeDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        if (database == null) return null;
        DatabaseVersion dbVersion = new DatabaseVersion();
        dbVersion.setDatabaseId(database.getId());
        dbVersion.setVersion(version);
        dbVersion.setDisplayName(displayName);
        dbVersion.setReleaseDate(releaseDate);
        dbVersion.setEndOfLifeDate(endOfLifeDate);
        dbVersion.setReleaseNotes("Oracle Database " + version + " with advanced features and performance enhancements");
        dbVersion.setIsSupported(isSupported);
        dbVersion.setIsRecommended(isRecommended);
        dbVersion.setActive(true);
        return dbVersion;
    }

    private DatabaseVersion createMSSQLVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        return createMSSQLVersion(database, version, displayName, releaseDate, null, isSupported, isRecommended);
    }

    private DatabaseVersion createMSSQLVersion(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        LocalDate endOfLifeDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        if (database == null) return null;
        DatabaseVersion dbVersion = new DatabaseVersion();
        dbVersion.setDatabaseId(database.getId());
        dbVersion.setVersion(version);
        dbVersion.setDisplayName(displayName);
        dbVersion.setReleaseDate(releaseDate);
        dbVersion.setEndOfLifeDate(endOfLifeDate);
        dbVersion.setReleaseNotes("SQL Server " + version + " with improved security and performance");
        dbVersion.setIsSupported(isSupported);
        dbVersion.setIsRecommended(isRecommended);
        dbVersion.setActive(true);
        return dbVersion;
    }

    private DatabaseVersion createDB2Version(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        return createDB2Version(database, version, displayName, releaseDate, null, isSupported, isRecommended);
    }

    private DatabaseVersion createDB2Version(
        Database database,
        String version,
        String displayName,
        LocalDate releaseDate,
        LocalDate endOfLifeDate,
        boolean isSupported,
        boolean isRecommended
    ) {
        if (database == null) return null;
        DatabaseVersion dbVersion = new DatabaseVersion();
        dbVersion.setDatabaseId(database.getId());
        dbVersion.setVersion(version);
        dbVersion.setDisplayName(displayName);
        dbVersion.setReleaseDate(releaseDate);
        dbVersion.setEndOfLifeDate(endOfLifeDate);
        dbVersion.setReleaseNotes("DB2 " + version + " with enhanced features and reliability improvements");
        dbVersion.setIsSupported(isSupported);
        dbVersion.setIsRecommended(isRecommended);
        dbVersion.setActive(true);
        return dbVersion;
    }
}

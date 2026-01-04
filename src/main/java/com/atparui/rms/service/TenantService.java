package com.atparui.rms.service;

import com.atparui.rms.domain.DatabaseVendor;
import com.atparui.rms.domain.Tenant;
import com.atparui.rms.repository.DatabaseVendorRepository;
import com.atparui.rms.repository.TenantRepository;
import com.atparui.rms.service.dto.TenantCreationContext;
import com.atparui.rms.service.dto.TenantDatabaseConfigDTO;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final com.atparui.rms.repository.TenantClientRepository tenantClientRepository;
    private final DatabaseVendorRepository databaseVendorRepository;
    private final KeycloakRealmService keycloakRealmService;
    private final DatabaseProvisioningService databaseProvisioningService;
    private final TenantLiquibaseService tenantLiquibaseService;
    private final TransactionalOperator transactionalOperator;
    private final ConcurrentHashMap<String, ConnectionFactory> connectionFactoryCache = new ConcurrentHashMap<>();

    @Value("${multitenancy.keycloak.base-url:https://rmsauth.atparui.com}")
    private String keycloakBaseUrl;

    public TenantService(
        TenantRepository tenantRepository,
        com.atparui.rms.repository.TenantClientRepository tenantClientRepository,
        DatabaseVendorRepository databaseVendorRepository,
        KeycloakRealmService keycloakRealmService,
        DatabaseProvisioningService databaseProvisioningService,
        TenantLiquibaseService tenantLiquibaseService,
        @Qualifier("masterTransactionManager") ReactiveTransactionManager masterTransactionManager
    ) {
        this.tenantRepository = tenantRepository;
        this.tenantClientRepository = tenantClientRepository;
        this.databaseVendorRepository = databaseVendorRepository;
        this.keycloakRealmService = keycloakRealmService;
        this.databaseProvisioningService = databaseProvisioningService;
        this.tenantLiquibaseService = tenantLiquibaseService;
        this.transactionalOperator = TransactionalOperator.create(masterTransactionManager);
    }

    public Mono<Tenant> findTenant(String tenantId) {
        return tenantRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    public Mono<Tenant> findByTenantKey(String tenantKey) {
        return tenantRepository.findByTenantKeyAndActiveTrue(tenantKey);
    }

    public Mono<Tenant> findBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomainAndActiveTrue(subdomain);
    }

    public Flux<Tenant> findAll() {
        return tenantRepository.findAll().filter(tenant -> !"gateway".equals(tenant.getTenantId())); // Filter out default tenant
    }

    public Mono<Tenant> findById(Long id) {
        return tenantRepository.findById(id);
    }

    public Mono<Tenant> save(Tenant tenant) {
        return save(tenant, false);
    }

    public Mono<Tenant> save(Tenant tenant, boolean applyLiquibaseImmediately) {
        if (tenant.getId() == null) {
            // Check if tenant ID or tenant key already exists
            return Mono.zip(
                tenantRepository.existsByTenantId(tenant.getTenantId()),
                tenantRepository.existsByTenantKey(tenant.getTenantKey())
            ).flatMap(tuple -> {
                boolean tenantIdExists = tuple.getT1();
                boolean tenantKeyExists = tuple.getT2();

                if (tenantIdExists) {
                    return Mono.error(new RuntimeException("Tenant ID already exists: " + tenant.getTenantId()));
                }
                if (tenantKeyExists) {
                    return Mono.error(new RuntimeException("Tenant Key already exists: " + tenant.getTenantKey()));
                }
                return createTenantWithKeycloak(tenant, applyLiquibaseImmediately);
            });
        } else {
            return tenantRepository.save(tenant).doOnSuccess(savedTenant -> clearCache(savedTenant.getTenantId()));
        }
    }

    private Mono<Tenant> createTenantWithKeycloak(Tenant tenant, boolean applyLiquibaseImmediately) {
        // Set default values based on tenant key
        if (tenant.getRealmName() == null || tenant.getRealmName().isEmpty()) {
            tenant.setRealmName(tenant.getTenantKey() + "_realm");
        }
        if (tenant.getClientId() == null || tenant.getClientId().isEmpty()) {
            tenant.setClientId(tenant.getTenantKey() + "_web");
        }
        if (tenant.getClientSecret() == null || tenant.getClientSecret().isEmpty()) {
            tenant.setClientSecret(generateClientSecret());
        }
        if (tenant.getDefaultRoles() == null || tenant.getDefaultRoles().isEmpty()) {
            tenant.setDefaultRoles(
                "ROLE_ADMIN,ROLE_MANAGER,ROLE_SUPERVISOR,ROLE_WAITER,ROLE_CHEF,ROLE_CASHIER,ROLE_CUSTOMER,ROLE_ANONYMOUS"
            );
        }

        // Set default database vendor code if not provided
        if (tenant.getDatabaseVendorCode() == null || tenant.getDatabaseVendorCode().isEmpty()) {
            tenant.setDatabaseVendorCode("POSTGRESQL");
        }

        // Set default provisioning mode if not provided
        if (tenant.getDatabaseProvisioningMode() == null || tenant.getDatabaseProvisioningMode().isEmpty()) {
            tenant.setDatabaseProvisioningMode("AUTO_CREATE");
        }

        // Validate database vendor exists and is active
        return databaseVendorRepository
            .findByVendorCodeAndActiveTrue(tenant.getDatabaseVendorCode())
            .switchIfEmpty(Mono.error(new RuntimeException("Invalid or inactive database vendor code: " + tenant.getDatabaseVendorCode())))
            .flatMap(vendor -> {
                // Generate database URL if not provided and we have host/port/database
                if (tenant.getDatabaseUrl() == null || tenant.getDatabaseUrl().isEmpty()) {
                    if ("USE_EXISTING".equals(tenant.getDatabaseProvisioningMode())) {
                        // Generate URL from host/port/database using vendor template
                        tenant.setDatabaseUrl(buildDatabaseUrl(vendor, tenant));
                    }
                }

                // Continue with tenant creation after validation
                return createTenantWithKeycloakInternal(tenant, applyLiquibaseImmediately, vendor);
            });
    }

    private Mono<Tenant> createTenantWithKeycloakInternal(Tenant tenant, boolean applyLiquibaseImmediately, DatabaseVendor vendor) {
        // Create context to track external resources for rollback (database, Keycloak)
        TenantCreationContext context = new TenantCreationContext(
            tenant.getTenantId() != null ? tenant.getTenantId() : tenant.getTenantKey(),
            tenant.getTenantKey()
        );

        // Wrap tenant database operations in a transaction
        // This ensures that if any step fails, the tenant entity save will be automatically rolled back
        Mono<Tenant> tenantCreationFlow = tenantRepository
            .save(tenant)
            .doOnSuccess(savedTenant -> {
                context.setTenantSaved(true);
                context.setTenantEntityId(savedTenant.getId());
                log.debug("Step 1: Saved tenant entity (within transaction): {}", savedTenant.getTenantKey());
            })
            // Step 2: Handle database provisioning based on mode
            .flatMap(savedTenant -> {
                if ("USE_EXISTING".equals(savedTenant.getDatabaseProvisioningMode())) {
                    // Use existing database - no need to create, just validate connection details are set
                    log.debug("Step 2: Using existing database for tenant: {}", savedTenant.getTenantKey());
                    if (savedTenant.getDatabaseUrl() == null || savedTenant.getDatabaseUrl().isEmpty()) {
                        return Mono.error(new RuntimeException("Database URL is required when using existing database"));
                    }
                    if (savedTenant.getDatabaseUsername() == null || savedTenant.getDatabaseUsername().isEmpty()) {
                        return Mono.error(new RuntimeException("Database username is required when using existing database"));
                    }
                    if (savedTenant.getDatabasePassword() == null || savedTenant.getDatabasePassword().isEmpty()) {
                        return Mono.error(new RuntimeException("Database password is required when using existing database"));
                    }
                    // Set database name and host/port if not already set
                    if (savedTenant.getDatabaseName() == null || savedTenant.getDatabaseName().isEmpty()) {
                        // Extract database name from URL if possible
                        String dbName = extractDatabaseNameFromUrl(savedTenant.getDatabaseUrl(), vendor);
                        savedTenant.setDatabaseName(dbName);
                    }
                    return Mono.just(savedTenant);
                } else {
                    // AUTO_CREATE mode - create database
                    return Mono.fromRunnable(() -> {
                        try {
                            databaseProvisioningService.createTenantDatabase(savedTenant.getTenantKey(), applyLiquibaseImmediately);
                            context.setDatabaseCreated(true);
                            log.debug("Step 2: Created database for tenant: {}", savedTenant.getTenantKey());
                        } catch (Exception e) {
                            log.error("Failed to create database for tenant: {}", savedTenant.getTenantKey(), e);
                            throw new RuntimeException("Failed to create database", e);
                        }
                    }).thenReturn(savedTenant);
                }
            })
            // Step 3: Update tenant with database info (within transaction)
            .flatMap(savedTenant -> {
                if ("AUTO_CREATE".equals(savedTenant.getDatabaseProvisioningMode())) {
                    // Set auto-generated database details
                    savedTenant.setDatabaseUrl(databaseProvisioningService.getTenantDatabaseUrl(savedTenant.getTenantKey()));
                    savedTenant.setDatabaseUsername(databaseProvisioningService.getTenantDatabaseUsername(savedTenant.getTenantKey()));
                    savedTenant.setDatabasePassword(databaseProvisioningService.getTenantDatabasePassword(savedTenant.getTenantKey()));
                    // Set host/port/database name for auto-created databases
                    if (savedTenant.getDatabaseHost() == null) {
                        savedTenant.setDatabaseHost("localhost"); // Default for auto-created
                    }
                    if (savedTenant.getDatabasePort() == null) {
                        savedTenant.setDatabasePort(vendor.getDefaultPort());
                    }
                    if (savedTenant.getDatabaseName() == null) {
                        savedTenant.setDatabaseName("rms_" + savedTenant.getTenantKey());
                    }
                }
                // For USE_EXISTING, details are already set from form
                return tenantRepository.save(savedTenant);
            })
            // Step 4: Create Keycloak realm (external resource - not part of transaction)
            .flatMap(savedTenant -> {
                return Mono.fromCallable(() -> {
                    try {
                        java.util.List<com.atparui.rms.service.KeycloakRealmService.ClientInfo> clientInfos =
                            keycloakRealmService.createTenantRealm(savedTenant.getTenantKey(), savedTenant.getName());
                        context.setRealmCreated(true);
                        context.setClientsCreated(true);
                        context.setRolesCreated(true);
                        context.setFlowsCreated(true);
                        log.debug(
                            "Step 4: Created Keycloak realm for tenant: {} with {} clients",
                            savedTenant.getTenantKey(),
                            clientInfos.size()
                        );
                        return clientInfos;
                    } catch (Exception e) {
                        log.error("Failed to create Keycloak realm for tenant: {}", savedTenant.getTenantKey(), e);
                        throw new RuntimeException("Failed to create Keycloak realm", e);
                    }
                }).flatMap(clientInfos -> {
                    // Step 4b: Save all client credentials to tenant_clients table (within transaction)
                    if (clientInfos != null && !clientInfos.isEmpty()) {
                        return Flux.fromIterable(clientInfos)
                            .map(clientInfo -> {
                                com.atparui.rms.domain.TenantClient tenantClient = new com.atparui.rms.domain.TenantClient(
                                    savedTenant.getId(),
                                    clientInfo.getClientId(),
                                    clientInfo.getClientSecret(),
                                    clientInfo.getClientType(),
                                    savedTenant.getRealmName()
                                );
                                return tenantClient;
                            })
                            .flatMap(tenantClientRepository::save)
                            .then(Mono.just(savedTenant));
                    }
                    return Mono.just(savedTenant);
                });
            })
            .doOnSuccess(savedTenant -> {
                log.info("Successfully created tenant with database and Keycloak realm: {}", savedTenant.getTenantKey());
            })
            // Rollback external resources on error
            // Note: Tenant entity rollback is handled automatically by the transaction
            .onErrorResume(error -> {
                log.error("Error during tenant creation, initiating rollback for external resources: {}", context.getTenantKey(), error);
                // Rollback external resources (database, Keycloak)
                // The tenant entity will be automatically rolled back by the transaction
                return rollbackExternalResources(context)
                    .then(
                        Mono.<Tenant>error(
                            new RuntimeException("Failed to create tenant: " + context.getTenantKey() + ". Rollback completed.", error)
                        )
                    )
                    .onErrorResume(rollbackError -> {
                        log.error("Error during rollback for tenant: {}", context.getTenantKey(), rollbackError);
                        // Even if rollback fails, we still want to propagate the original error
                        return Mono.<Tenant>error(
                            new RuntimeException(
                                "Failed to create tenant: " + context.getTenantKey() + ". Rollback attempted but failed.",
                                error
                            )
                        );
                    });
            });

        // Execute the flow within a transaction
        // If any error occurs, the transaction will automatically rollback the tenant entity save/update
        return transactionalOperator.transactional(tenantCreationFlow);
    }

    /**
     * Rollback external resources created during tenant creation (database, Keycloak realm).
     * Note: The tenant entity rollback is handled automatically by the database transaction.
     *
     * @param context the tenant creation context
     * @return Mono that completes when rollback is finished
     */
    private Mono<Void> rollbackExternalResources(TenantCreationContext context) {
        log.info(
            "Starting rollback of external resources for tenant: {} (database: {}, realm: {})",
            context.getTenantKey(),
            context.isDatabaseCreated(),
            context.isRealmCreated()
        );

        // Rollback in reverse order of creation
        Mono<Void> deleteRealm = Mono.empty();
        if (context.isRealmCreated() || context.isClientsCreated() || context.isRolesCreated() || context.isFlowsCreated()) {
            deleteRealm = Mono.fromRunnable(() -> {
                try {
                    // Use tenantKey for realm deletion since realm is created with tenantKey
                    keycloakRealmService.deleteTenantRealm(context.getTenantKey());
                    log.info("Rollback: Deleted Keycloak realm: {}", context.getRealmName());
                } catch (Exception e) {
                    // Realm may have already been deleted during its own rollback, or may not exist
                    log.warn(
                        "Rollback: Failed to delete Keycloak realm: {} (may already be deleted), continuing rollback",
                        context.getRealmName(),
                        e
                    );
                }
            })
                .onErrorResume(e -> {
                    log.warn("Rollback: Error deleting Keycloak realm, continuing", e);
                    return Mono.<Void>empty();
                })
                .then();
        }

        Mono<Void> deleteDatabase = Mono.empty();
        if (context.isDatabaseCreated()) {
            deleteDatabase = Mono.fromRunnable(() -> {
                try {
                    log.info(
                        "Rollback: Attempting to delete database: {} and user: {}",
                        context.getDatabaseName(),
                        context.getDatabaseUser()
                    );
                    databaseProvisioningService.deleteTenantDatabase(context.getTenantKey());
                    log.info(
                        "Rollback: Successfully deleted database: {} and user: {}",
                        context.getDatabaseName(),
                        context.getDatabaseUser()
                    );
                } catch (Exception e) {
                    // Database may have already been deleted during its own rollback, or may not exist
                    // But we should log this as an error since it's important
                    log.error(
                        "Rollback: CRITICAL - Failed to delete database: {} and user: {}. Manual cleanup may be required. Error: {}",
                        context.getDatabaseName(),
                        context.getDatabaseUser(),
                        e.getMessage(),
                        e
                    );
                    // Re-throw to ensure the error is visible, but the onErrorResume will catch it
                    throw new RuntimeException("Failed to delete database during rollback: " + context.getDatabaseName(), e);
                }
            })
                .onErrorResume(e -> {
                    log.error(
                        "Rollback: Error deleting database: {} and user: {}. This is a critical error that requires manual cleanup.",
                        context.getDatabaseName(),
                        context.getDatabaseUser(),
                        e
                    );
                    // Continue with other rollback operations, but log as error
                    return Mono.<Void>empty();
                })
                .then();
        } else {
            log.debug(
                "Rollback: Database was not marked as created in context, skipping database deletion for tenant: {}",
                context.getTenantKey()
            );
        }

        // Execute rollback steps sequentially
        // Note: Tenant entity rollback is handled automatically by the transaction, so we don't need to delete it here
        return deleteRealm
            .doOnError(e -> log.error("Rollback: Error deleting realm, but continuing with database deletion", e))
            .onErrorResume(e -> {
                log.warn("Rollback: Continuing rollback despite realm deletion error");
                return Mono.empty();
            })
            .then(deleteDatabase)
            .doOnError(e -> log.error("Rollback: Error deleting database", e))
            .onErrorResume(e -> {
                log.warn("Rollback: Continuing despite database deletion error");
                return Mono.empty();
            })
            .doOnSuccess(v -> log.info("Rollback of external resources completed for tenant: {}", context.getTenantKey()))
            .doOnError(e -> log.error("Rollback: Error during external resources rollback for tenant: {}", context.getTenantKey(), e))
            .onErrorResume(e -> {
                log.error("Rollback: External resources rollback failed for tenant: {}", context.getTenantKey(), e);
                // Return empty to allow the rollback to complete, but the error is logged
                return Mono.empty();
            });
    }

    /**
     * Extract database name from JDBC URL.
     *
     * @param jdbcUrl the JDBC URL
     * @param vendor the database vendor
     * @return database name or default
     */
    private String extractDatabaseNameFromUrl(String jdbcUrl, DatabaseVendor vendor) {
        try {
            if (vendor.getVendorCode().equals("MSSQL")) {
                // MSSQL: jdbc:sqlserver://host:port;databaseName=dbname
                int dbNameIndex = jdbcUrl.indexOf("databaseName=");
                if (dbNameIndex > 0) {
                    String dbPart = jdbcUrl.substring(dbNameIndex + "databaseName=".length());
                    int endIndex = dbPart.indexOf(";");
                    if (endIndex > 0) {
                        return dbPart.substring(0, endIndex);
                    }
                    return dbPart;
                }
            } else {
                // Standard format: jdbc:driver://host:port/database
                String[] parts = jdbcUrl.split("/");
                if (parts.length > 0) {
                    String lastPart = parts[parts.length - 1];
                    // Remove query parameters
                    int queryIndex = lastPart.indexOf("?");
                    if (queryIndex > 0) {
                        return lastPart.substring(0, queryIndex);
                    }
                    return lastPart;
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract database name from URL: {}", jdbcUrl, e);
        }
        return "rms_database"; // Default fallback
    }

    /**
     * Build database URL from vendor template and tenant connection details.
     *
     * @param vendor the database vendor
     * @param tenant the tenant with connection details
     * @return JDBC URL string
     */
    private String buildDatabaseUrl(DatabaseVendor vendor, Tenant tenant) {
        String template = vendor.getJdbcUrlTemplate();
        if (template == null || template.isEmpty()) {
            // Fallback to default format
            template = "jdbc:" + vendor.getDriverKey() + "://{host}:{port}/{database}";
        }

        String host = tenant.getDatabaseHost() != null ? tenant.getDatabaseHost() : "localhost";
        Integer port = tenant.getDatabasePort() != null ? tenant.getDatabasePort() : vendor.getDefaultPort();
        String database = tenant.getDatabaseName() != null ? tenant.getDatabaseName() : "rms_" + tenant.getTenantKey();

        String url = template.replace("{host}", host).replace("{port}", String.valueOf(port));

        // Handle database name replacement
        if (url.contains("{database}")) {
            url = url.replace("{database}", database);
        } else if (vendor.getVendorCode().equals("MSSQL")) {
            // MSSQL uses databaseName parameter
            url += ";databaseName=" + database;
        }

        // Add schema if provided and vendor supports it
        if (tenant.getSchemaName() != null && !tenant.getSchemaName().isEmpty()) {
            if (vendor.getVendorCode().equals("POSTGRESQL")) {
                url += "?currentSchema=" + tenant.getSchemaName();
            } else if (vendor.getVendorCode().equals("ORACLE")) {
                if (!url.contains("?")) {
                    url += "?current_schema=" + tenant.getSchemaName();
                } else {
                    url += "&current_schema=" + tenant.getSchemaName();
                }
            }
        }

        return url;
    }

    private String generateClientSecret() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    public Mono<Void> delete(Long id) {
        return tenantRepository
            .findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Tenant not found with ID: " + id)))
            .flatMap(tenant -> {
                String tenantId = tenant.getTenantId();
                String tenantKey = tenant.getTenantKey();
                log.info("Starting deletion process for tenant: {} (ID: {})", tenantKey, id);

                // Delete in order: Keycloak realm -> Database (includes user) -> Tenant clients -> Tenant entity
                return Mono.fromRunnable(() -> {
                    try {
                        // 1. Delete Keycloak realm (includes clients, roles, flows)
                        // Use tenantKey to match the realm name created during tenant creation
                        log.debug("Deleting Keycloak realm for tenant: {}", tenantKey);
                        keycloakRealmService.deleteTenantRealm(tenantKey);
                        log.info("Successfully deleted Keycloak realm for tenant: {}", tenantKey);
                    } catch (Exception e) {
                        log.error("Failed to delete Keycloak realm for tenant: {}", tenantKey, e);
                    }
                })
                    .then(
                        Mono.fromRunnable(() -> {
                            try {
                                // 2. Delete tenant database and database user
                                // This method handles both database and user deletion
                                log.debug("Deleting database and user for tenant: {}", tenantKey);
                                databaseProvisioningService.deleteTenantDatabase(tenantKey);
                                log.info("Successfully deleted database and user for tenant: {}", tenantKey);
                            } catch (Exception e) {
                                log.error("Failed to delete database/user for tenant: {}", tenantKey, e);
                            }
                        })
                    )
                    .then(
                        // 3. Delete tenant clients from database (before deleting tenant entity)
                        tenantClientRepository
                            .deleteByTenantId(tenant.getId())
                            .doOnSuccess(v -> log.debug("Deleted tenant clients for tenant: {}", tenantKey))
                            .doOnError(e -> log.warn("Failed to delete tenant clients for tenant: {}, continuing", tenantKey, e))
                            .onErrorResume(e -> Mono.empty()) // Continue even if client deletion fails
                    )
                    .then(
                        Mono.fromRunnable(() -> {
                            // 4. Clear cache
                            clearCache(tenantId);
                        })
                    )
                    .then(tenantRepository.deleteById(id))
                    .doOnSuccess(v -> log.info("Successfully deleted tenant entity with ID: {} (tenantKey: {})", id, tenantKey))
                    .doOnError(error -> log.error("Failed to delete tenant with ID: {} (tenantKey: {})", id, tenantKey, error));
            })
            .then();
    }

    public Mono<ConnectionFactory> getConnectionFactory(String tenantId) {
        return findTenant(tenantId).map(tenant -> connectionFactoryCache.computeIfAbsent(tenantId, key -> createConnectionFactory(tenant)));
    }

    private ConnectionFactory createConnectionFactory(Tenant tenant) {
        String[] urlParts = tenant.getDatabaseUrl().replace("jdbc:postgresql://", "").split("/");
        String[] hostPort = urlParts[0].split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 5432;
        String database = urlParts[1];

        return new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(tenant.getDatabaseUsername())
                .password(tenant.getDatabasePassword())
                .schema(tenant.getSchemaName())
                .build()
        );
    }

    public void clearCache(String tenantId) {
        connectionFactoryCache.remove(tenantId);
    }

    /**
     * Get tenant database configuration for the given tenant ID.
     * Converts Tenant entity to TenantDatabaseConfigDTO with R2DBC URL format and includes all clients.
     *
     * @param tenantId the tenant ID
     * @return Mono containing TenantDatabaseConfigDTO
     */
    public Mono<TenantDatabaseConfigDTO> getTenantDatabaseConfig(String tenantId) {
        return findTenant(tenantId).flatMap(tenant ->
            tenantClientRepository
                .findByTenantId(tenant.getId())
                .collectList()
                .map(clients -> {
                    return convertToDatabaseConfigDTO(tenant, clients);
                })
        );
    }

    /**
     * Apply Liquibase changes to a tenant database.
     *
     * @param tenantId the tenant ID
     * @return Mono that completes when Liquibase changes are applied
     */
    public Mono<Void> applyLiquibaseChanges(String tenantId) {
        return findTenant(tenantId)
            .flatMap(tenant -> {
                try {
                    tenantLiquibaseService.applyLiquibaseChanges(
                        tenant.getTenantId(),
                        tenant.getDatabaseUrl(),
                        tenant.getDatabaseUsername(),
                        tenant.getDatabasePassword()
                    );
                    log.info("Successfully applied Liquibase changes for tenant: {}", tenantId);
                    return Mono.empty();
                } catch (Exception e) {
                    log.error("Failed to apply Liquibase changes for tenant: {}", tenantId, e);
                    return Mono.error(new RuntimeException("Failed to apply Liquibase changes", e));
                }
            })
            .then();
    }

    /**
     * Convert Tenant entity to TenantDatabaseConfigDTO.
     * Converts JDBC URL format to R2DBC URL format if needed and includes all clients.
     *
     * @param tenant the tenant entity
     * @param clients the list of tenant clients
     * @return TenantDatabaseConfigDTO
     */
    private TenantDatabaseConfigDTO convertToDatabaseConfigDTO(Tenant tenant, java.util.List<com.atparui.rms.domain.TenantClient> clients) {
        String databaseUrl = tenant.getDatabaseUrl();

        // Convert JDBC URL to R2DBC URL format if needed
        if (databaseUrl != null && databaseUrl.startsWith("jdbc:postgresql://")) {
            databaseUrl = databaseUrl.replace("jdbc:postgresql://", "r2dbc:postgresql://");
        } else if (databaseUrl != null && !databaseUrl.startsWith("r2dbc:")) {
            // If it's not in R2DBC format, assume it needs conversion
            // This handles cases where URL might be stored without prefix
            if (!databaseUrl.contains("://")) {
                databaseUrl = "r2dbc:postgresql://" + databaseUrl;
            } else {
                databaseUrl = "r2dbc:" + databaseUrl.substring(databaseUrl.indexOf("://"));
            }
        }

        TenantDatabaseConfigDTO dto = new TenantDatabaseConfigDTO(
            tenant.getTenantId(),
            databaseUrl,
            tenant.getDatabaseUsername(),
            tenant.getDatabasePassword(),
            20, // default maxPoolSize
            30000, // default connectionTimeout
            "SELECT 1" // default validationQuery
        );

        // Set Keycloak configuration
        dto.setKeycloakBaseUrl(keycloakBaseUrl);
        dto.setRealmName(tenant.getRealmName() != null ? tenant.getRealmName() : tenant.getTenantId() + "_realm");

        // Set clients list
        java.util.List<com.atparui.rms.service.dto.TenantClientDTO> clientDTOs = clients
            .stream()
            .map(client ->
                new com.atparui.rms.service.dto.TenantClientDTO(client.getClientId(), client.getClientSecret(), client.getClientType())
            )
            .collect(java.util.stream.Collectors.toList());
        dto.setClients(clientDTOs);

        return dto;
    }
}

package com.atparui.rms.service;

import com.atparui.rms.domain.Tenant;
import com.atparui.rms.repository.TenantRepository;
import com.atparui.rms.service.dto.TenantCreationContext;
import com.atparui.rms.service.dto.TenantDatabaseConfigDTO;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final KeycloakRealmService keycloakRealmService;
    private final DatabaseProvisioningService databaseProvisioningService;
    private final TenantLiquibaseService tenantLiquibaseService;
    private final ConcurrentHashMap<String, ConnectionFactory> connectionFactoryCache = new ConcurrentHashMap<>();

    public TenantService(
        TenantRepository tenantRepository,
        KeycloakRealmService keycloakRealmService,
        DatabaseProvisioningService databaseProvisioningService,
        TenantLiquibaseService tenantLiquibaseService
    ) {
        this.tenantRepository = tenantRepository;
        this.keycloakRealmService = keycloakRealmService;
        this.databaseProvisioningService = databaseProvisioningService;
        this.tenantLiquibaseService = tenantLiquibaseService;
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

        // Create context to track resources for rollback
        TenantCreationContext context = new TenantCreationContext(
            tenant.getTenantId() != null ? tenant.getTenantId() : tenant.getTenantKey(),
            tenant.getTenantKey()
        );

        // Step 1: Save tenant entity
        return tenantRepository
            .save(tenant)
            .doOnSuccess(savedTenant -> {
                context.setTenantSaved(true);
                context.setTenantEntityId(savedTenant.getId());
                log.debug("Step 1: Saved tenant entity: {}", savedTenant.getTenantKey());
            })
            // Step 2: Create database
            .flatMap(savedTenant ->
                Mono.fromRunnable(() -> {
                    try {
                        databaseProvisioningService.createTenantDatabase(savedTenant.getTenantKey(), applyLiquibaseImmediately);
                        context.setDatabaseCreated(true);
                        log.debug("Step 2: Created database for tenant: {}", savedTenant.getTenantKey());
                    } catch (Exception e) {
                        log.error("Failed to create database for tenant: {}", savedTenant.getTenantKey(), e);
                        // Database creation has its own rollback logic, but we still need to mark context
                        // The exception will trigger the doOnError handler which will attempt rollback
                        throw new RuntimeException("Failed to create database", e);
                    }
                }).thenReturn(savedTenant)
            )
            // Step 3: Update tenant with database info
            .flatMap(savedTenant -> {
                savedTenant.setDatabaseUrl(databaseProvisioningService.getTenantDatabaseUrl(savedTenant.getTenantKey()));
                savedTenant.setDatabaseUsername(databaseProvisioningService.getTenantDatabaseUsername(savedTenant.getTenantKey()));
                savedTenant.setDatabasePassword(databaseProvisioningService.getTenantDatabasePassword(savedTenant.getTenantKey()));
                return tenantRepository.save(savedTenant);
            })
            // Step 4: Create Keycloak realm
            .flatMap(savedTenant ->
                Mono.fromRunnable(() -> {
                    try {
                        keycloakRealmService.createTenantRealm(savedTenant.getTenantKey(), savedTenant.getName());
                        context.setRealmCreated(true);
                        context.setClientsCreated(true);
                        context.setRolesCreated(true);
                        context.setFlowsCreated(true);
                        log.debug("Step 4: Created Keycloak realm for tenant: {}", savedTenant.getTenantKey());
                    } catch (Exception e) {
                        log.error("Failed to create Keycloak realm for tenant: {}", savedTenant.getTenantKey(), e);
                        // Keycloak realm creation has its own rollback logic, but we need to ensure database is also rolled back
                        // The exception will trigger the doOnError handler which will rollback database
                        throw new RuntimeException("Failed to create Keycloak realm", e);
                    }
                }).thenReturn(savedTenant)
            )
            .doOnSuccess(savedTenant -> {
                log.info("Successfully created tenant with database and Keycloak realm: {}", savedTenant.getTenantKey());
            })
            // Rollback on error - MUST complete synchronously before error is propagated
            .onErrorResume(error -> {
                log.error("Error during tenant creation, initiating rollback for tenant: {}", context.getTenantKey(), error);
                // Execute rollback synchronously and wait for it to complete
                // Use doFinally to ensure tenant deletion is attempted even if rollback fails
                return rollbackTenantCreation(context)
                    .doFinally(signalType -> {
                        // Final safety check: Try to delete tenant entity one more time if rollback didn't complete successfully
                        // This is a last resort to ensure cleanup
                        if (context.isTenantSaved()) {
                            log.debug("Rollback: Final safety check - attempting tenant entity deletion for: {}", context.getTenantKey());
                            try {
                                // Try synchronous deletion as a last resort
                                if (context.getTenantEntityId() != null) {
                                    tenantRepository.deleteById(context.getTenantEntityId()).block(); // Block to ensure it completes
                                    log.info(
                                        "Rollback: Final safety check - deleted tenant entity with ID: {}",
                                        context.getTenantEntityId()
                                    );
                                } else {
                                    // Try by tenantKey
                                    tenantRepository
                                        .findByTenantKey(context.getTenantKey())
                                        .flatMap(tenantEntity -> tenantRepository.deleteById(tenantEntity.getId()))
                                        .block(); // Block to ensure it completes
                                    log.info("Rollback: Final safety check - deleted tenant entity by key: {}", context.getTenantKey());
                                }
                            } catch (Exception finalError) {
                                log.error(
                                    "Rollback: CRITICAL - Final safety check failed to delete tenant entity for: {}. Manual cleanup REQUIRED.",
                                    context.getTenantKey(),
                                    finalError
                                );
                            }
                        }
                    })
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
    }

    /**
     * Rollback tenant creation by removing all created resources in reverse order.
     * This ensures that if any step fails, all previously created resources are cleaned up.
     *
     * @param context the tenant creation context
     * @return Mono that completes when rollback is finished
     */
    private Mono<Void> rollbackTenantCreation(TenantCreationContext context) {
        log.info(
            "Starting rollback for tenant: {} (database: {}, realm: {})",
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

        // Delete tenant entity from database
        // ALWAYS try to delete the tenant entity if it was saved, regardless of context state
        // This ensures cleanup even if context is incomplete or incorrect
        Mono<Void> deleteTenant = Mono.defer(() -> {
            // Only attempt deletion if tenant was actually saved
            if (!context.isTenantSaved()) {
                log.debug("Rollback: Tenant entity was not saved, skipping deletion for tenantKey: {}", context.getTenantKey());
                return Mono.empty();
            }

            log.info(
                "Rollback: Attempting to delete tenant entity for tenantKey: {} (saved: true, entityId: {})",
                context.getTenantKey(),
                context.getTenantEntityId()
            );

            // Strategy 1: If we have the entity ID, try deleting by ID first (most reliable)
            if (context.getTenantEntityId() != null) {
                log.debug("Rollback: Attempting to delete tenant by ID: {}", context.getTenantEntityId());
                return tenantRepository
                    .deleteById(context.getTenantEntityId())
                    .doOnSuccess(v -> log.info("Rollback: Successfully deleted tenant entity with ID: {}", context.getTenantEntityId()))
                    .onErrorResume(e -> {
                        log.warn(
                            "Rollback: Failed to delete tenant entity with ID: {} (error: {}), trying fallback method",
                            context.getTenantEntityId(),
                            e.getMessage()
                        );
                        // Fall through to Strategy 2 - don't return empty, continue to fallback
                        return Mono.empty();
                    })
                    .then(
                        // Strategy 2: Always try to find and delete by tenantKey as a fallback/verification
                        Mono.defer(() -> {
                            log.debug("Rollback: Verifying deletion by checking if tenant still exists by key: {}", context.getTenantKey());
                            return tenantRepository
                                .findByTenantKey(context.getTenantKey())
                                .flatMap(tenantEntity -> {
                                    log.warn(
                                        "Rollback: Tenant entity still exists after ID deletion (ID: {}), deleting by key",
                                        tenantEntity.getId()
                                    );
                                    return tenantRepository.deleteById(tenantEntity.getId());
                                })
                                .doOnSuccess(v ->
                                    log.info(
                                        "Rollback: Successfully deleted tenant entity by tenantKey (fallback): {}",
                                        context.getTenantKey()
                                    )
                                )
                                .switchIfEmpty(
                                    Mono.fromRunnable(() ->
                                        log.info(
                                            "Rollback: Tenant entity confirmed deleted (not found by tenantKey: {})",
                                            context.getTenantKey()
                                        )
                                    ).then()
                                )
                                .onErrorResume(fallbackError -> {
                                    log.error(
                                        "Rollback: CRITICAL - Failed to delete tenant entity by tenantKey: {} (error: {}). Manual cleanup required.",
                                        context.getTenantKey(),
                                        fallbackError.getMessage(),
                                        fallbackError
                                    );
                                    // Don't return empty - we want to know about this failure
                                    return Mono.empty();
                                });
                        })
                    );
            } else {
                // Strategy 2: If no ID, try finding and deleting by tenantKey
                log.debug("Rollback: No entity ID in context, attempting to delete by tenantKey: {}", context.getTenantKey());
                return tenantRepository
                    .findByTenantKey(context.getTenantKey())
                    .flatMap(tenantEntity -> {
                        log.info("Rollback: Found tenant by key, deleting with ID: {}", tenantEntity.getId());
                        return tenantRepository.deleteById(tenantEntity.getId());
                    })
                    .doOnSuccess(v -> log.info("Rollback: Successfully deleted tenant entity by tenantKey: {}", context.getTenantKey()))
                    .switchIfEmpty(
                        Mono.fromRunnable(() ->
                            log.warn(
                                "Rollback: Tenant entity not found by tenantKey: {} (was marked as saved but not found - may have been deleted or save failed)",
                                context.getTenantKey()
                            )
                        ).then()
                    )
                    .onErrorResume(e -> {
                        log.error(
                            "Rollback: CRITICAL - Failed to delete tenant entity by tenantKey: {} (error: {}). Manual cleanup required.",
                            context.getTenantKey(),
                            e.getMessage(),
                            e
                        );
                        // Don't return empty - we want to know about this failure
                        return Mono.empty();
                    });
            }
        }).then();

        // Execute rollback steps sequentially, but ensure tenant deletion happens even if other steps fail
        // Use doOnError to ensure tenant deletion is attempted even if realm/database deletion fails
        return deleteRealm
            .doOnError(e -> log.error("Rollback: Error deleting realm, but continuing with database and tenant deletion", e))
            .onErrorResume(e -> {
                log.warn("Rollback: Continuing rollback despite realm deletion error");
                return Mono.empty();
            })
            .then(deleteDatabase)
            .doOnError(e -> log.error("Rollback: Error deleting database, but continuing with tenant deletion", e))
            .onErrorResume(e -> {
                log.warn("Rollback: Continuing rollback despite database deletion error");
                return Mono.empty();
            })
            .then(deleteTenant)
            .doOnSuccess(v -> log.info("Rollback completed successfully for tenant: {}", context.getTenantKey()))
            .doOnError(e ->
                log.error("Rollback: CRITICAL - Failed to delete tenant entity during rollback for tenant: {}", context.getTenantKey(), e)
            )
            // Even if tenant deletion fails, we still want to complete the rollback attempt
            // The error is already logged, and we don't want to block the error propagation
            .onErrorResume(e -> {
                log.error("Rollback: Final rollback step (tenant deletion) failed for tenant: {}", context.getTenantKey(), e);
                // Return empty to allow the rollback to complete, but the error is logged
                return Mono.empty();
            });
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

                // Delete in order: Keycloak realm -> Database -> Tenant entity
                return Mono.fromRunnable(() -> {
                    try {
                        // 1. Delete Keycloak realm (includes clients, roles, flows)
                        log.debug("Deleting Keycloak realm for tenant: {}", tenantId);
                        keycloakRealmService.deleteTenantRealm(tenantId);
                        log.info("Successfully deleted Keycloak realm for tenant: {}", tenantId);
                    } catch (Exception e) {
                        log.warn("Failed to delete Keycloak realm for tenant: {}, continuing with deletion", tenantId, e);
                    }
                })
                    .then(
                        Mono.fromRunnable(() -> {
                            try {
                                // 2. Delete tenant database
                                log.debug("Deleting database for tenant: {}", tenantKey);
                                databaseProvisioningService.deleteTenantDatabase(tenantKey);
                                log.info("Successfully deleted database for tenant: {}", tenantKey);
                            } catch (Exception e) {
                                log.warn("Failed to delete database for tenant: {}, continuing with deletion", tenantKey, e);
                            }
                        })
                    )
                    .then(
                        Mono.fromRunnable(() -> {
                            // 3. Clear cache
                            clearCache(tenantId);
                        })
                    )
                    .then(tenantRepository.deleteById(id))
                    .doOnSuccess(v -> log.info("Successfully deleted tenant entity with ID: {}", id))
                    .doOnError(error -> log.error("Failed to delete tenant with ID: {}", id, error));
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
     * Converts Tenant entity to TenantDatabaseConfigDTO with R2DBC URL format.
     *
     * @param tenantId the tenant ID
     * @return Mono containing TenantDatabaseConfigDTO
     */
    public Mono<TenantDatabaseConfigDTO> getTenantDatabaseConfig(String tenantId) {
        return findTenant(tenantId).map(this::convertToDatabaseConfigDTO);
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
     * Converts JDBC URL format to R2DBC URL format if needed.
     *
     * @param tenant the tenant entity
     * @return TenantDatabaseConfigDTO
     */
    private TenantDatabaseConfigDTO convertToDatabaseConfigDTO(Tenant tenant) {
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

        return new TenantDatabaseConfigDTO(
            tenant.getTenantId(),
            databaseUrl,
            tenant.getDatabaseUsername(),
            tenant.getDatabasePassword(),
            20, // default maxPoolSize
            30000, // default connectionTimeout
            "SELECT 1" // default validationQuery
        );
    }
}

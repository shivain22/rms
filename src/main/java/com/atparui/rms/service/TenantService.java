package com.atparui.rms.service;

import com.atparui.rms.domain.Tenant;
import com.atparui.rms.repository.TenantRepository;
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

        return tenantRepository
            .save(tenant)
            .doOnSuccess(savedTenant -> {
                try {
                    // Create tenant database first and get connection details
                    databaseProvisioningService.createTenantDatabase(savedTenant.getTenantKey(), applyLiquibaseImmediately);

                    // Update tenant with database connection info
                    savedTenant.setDatabaseUrl(databaseProvisioningService.getTenantDatabaseUrl(savedTenant.getTenantKey()));
                    savedTenant.setDatabaseUsername(databaseProvisioningService.getTenantDatabaseUsername(savedTenant.getTenantKey()));
                    savedTenant.setDatabasePassword(databaseProvisioningService.getTenantDatabasePassword(savedTenant.getTenantKey()));

                    // Save updated tenant with database details
                    tenantRepository.save(savedTenant).subscribe();

                    // Create Keycloak realm, clients, and roles
                    keycloakRealmService.createTenantRealm(savedTenant.getTenantKey(), savedTenant.getName());

                    log.info("Successfully created tenant with database and Keycloak realm: {}", savedTenant.getTenantKey());
                } catch (Exception e) {
                    log.error("Failed to create tenant infrastructure for: {}", savedTenant.getTenantKey(), e);
                }
            })
            .doOnError(error -> {
                log.error("Failed to create tenant: {}", tenant.getTenantKey(), error);
            });
    }

    private String generateClientSecret() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    public Mono<Void> delete(Long id) {
        return tenantRepository
            .findById(id)
            .doOnNext(tenant -> {
                try {
                    // Delete Keycloak realm when tenant is deleted
                    keycloakRealmService.deleteTenantRealm(tenant.getTenantId());
                    log.info("Successfully deleted Keycloak realm for tenant: {}", tenant.getTenantId());

                    // Delete tenant database
                    databaseProvisioningService.deleteTenantDatabase(tenant.getTenantId());
                    log.info("Successfully deleted database for tenant: {}", tenant.getTenantId());
                } catch (Exception e) {
                    log.error("Failed to delete tenant infrastructure for: {}", tenant.getTenantId(), e);
                }
                clearCache(tenant.getTenantId());
            })
            .then(tenantRepository.deleteById(id));
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

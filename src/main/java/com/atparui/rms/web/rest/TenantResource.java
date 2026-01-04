package com.atparui.rms.web.rest;

import com.atparui.rms.domain.Tenant;
import com.atparui.rms.service.DatabaseConnectionTestService;
import com.atparui.rms.service.TenantService;
import com.atparui.rms.service.dto.DatabaseConnectionTestDTO;
import com.atparui.rms.service.dto.DatabaseConnectionTestResult;
import com.atparui.rms.service.dto.TenantDatabaseConfigDTO;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

@RestController
@RequestMapping("/api/tenants")
public class TenantResource {

    private static final Logger log = LoggerFactory.getLogger(TenantResource.class);
    private static final String ENTITY_NAME = "tenant";
    private final TenantService tenantService;
    private final DatabaseConnectionTestService databaseConnectionTestService;

    public TenantResource(TenantService tenantService, DatabaseConnectionTestService databaseConnectionTestService) {
        this.tenantService = tenantService;
        this.databaseConnectionTestService = databaseConnectionTestService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Tenant>> createTenant(
        @Valid @RequestBody Tenant tenant,
        @RequestParam(value = "applyLiquibase", defaultValue = "false") boolean applyLiquibase
    ) throws URISyntaxException {
        if (tenant.getId() != null) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(
                        HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idexists", "A new tenant cannot already have an ID")
                    )
                    .build()
            );
        }

        // Set tenantId from tenantKey if not provided
        if (tenant.getTenantId() == null || tenant.getTenantId().isEmpty()) {
            tenant.setTenantId(tenant.getTenantKey());
        }
        return tenantService
            .save(tenant, applyLiquibase)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/tenants/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            })
            .onErrorResume(throwable -> {
                String errorMessage = throwable.getMessage();
                String errorKey = "tenantexists";

                if (errorMessage.contains("Tenant Key already exists")) {
                    errorKey = "tenantkeyexists";
                } else if (errorMessage.contains("Tenant ID already exists")) {
                    errorKey = "tenantidexists";
                }

                return Mono.just(
                    ResponseEntity.badRequest()
                        .headers(HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, errorKey, errorMessage))
                        .build()
                );
            });
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Tenant>> updateTenant(@PathVariable Long id, @Valid @RequestBody Tenant tenant) {
        if (tenant.getId() == null || !tenant.getId().equals(id)) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idinvalid", "Invalid ID"))
                    .build()
            );
        }
        return tenantService
            .save(tenant)
            .map(result ->
                ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                    .body(result)
            )
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Flux<Tenant> getAllTenants() {
        return tenantService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Tenant>> getTenant(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(tenantService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> deleteTenant(@PathVariable Long id) {
        return tenantService
            .delete(id)
            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert("rmsApp", false, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }

    /**
     * {@code GET /api/tenants/{tenantId}/database-config} : Get database configuration for a tenant.
     * This endpoint is used by services (like RMS Service) to retrieve database connection information.
     * No authentication required as this is called by internal services.
     *
     * @param tenantId the tenant ID
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the tenant database configuration.
     */
    @GetMapping("/{tenantId}/database-config")
    public Mono<ResponseEntity<TenantDatabaseConfigDTO>> getTenantDatabaseConfig(@PathVariable String tenantId) {
        log.debug("REST request to get database config for tenant: {}", tenantId);
        return ResponseUtil.wrapOrNotFound(tenantService.getTenantDatabaseConfig(tenantId));
    }

    /**
     * {@code POST /api/tenants/{tenantId}/apply-liquibase} : Apply Liquibase changes to a tenant database.
     * This endpoint pulls the latest changes from the rms-service repository and applies them to the tenant database.
     *
     * @param tenantId the tenant ID
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} if successful, or {@code 404 (Not Found)} if tenant doesn't exist.
     */
    @PostMapping("/{tenantId}/apply-liquibase")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> applyLiquibaseChanges(@PathVariable String tenantId) {
        log.debug("REST request to apply Liquibase changes for tenant: {}", tenantId);
        return tenantService
            .applyLiquibaseChanges(tenantId)
            .then(Mono.just(ResponseEntity.ok().<Void>build()))
            .onErrorResume(throwable -> {
                log.error("Failed to apply Liquibase changes for tenant: {}", tenantId, throwable);
                return Mono.just(ResponseEntity.badRequest().<Void>build());
            });
    }

    /**
     * {@code POST /api/tenants/{tenantId}/invalidate-cache} : Invalidate cache for a tenant configuration.
     * This endpoint notifies the RMS Service to clear its cache for a specific tenant.
     * Useful when tenant configuration is updated, OAuth2 client secrets are rotated, or realm is deleted/renamed.
     *
     * @param tenantId the tenant ID
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} if successful, or {@code 404 (Not Found)} if tenant doesn't exist.
     */
    @PostMapping("/{tenantId}/invalidate-cache")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> invalidateCache(@PathVariable String tenantId) {
        log.debug("REST request to invalidate cache for tenant: {}", tenantId);
        return tenantService
            .findTenant(tenantId)
            .doOnNext(tenant -> {
                tenantService.clearCache(tenantId);
                log.info("Cache invalidated for tenant: {}", tenantId);
            })
            .then(Mono.just(ResponseEntity.ok().<Void>build()))
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().<Void>build()));
    }

    /**
     * {@code POST /api/tenants/test-database-connection} : Test database connection before tenant creation.
     * This endpoint tests the database connection using provided credentials.
     *
     * @param testDTO the database connection test DTO
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and test result.
     */
    @PostMapping("/test-database-connection")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<DatabaseConnectionTestResult>> testDatabaseConnection(
        @Valid @RequestBody DatabaseConnectionTestDTO testDTO
    ) {
        log.debug("REST request to test database connection for vendor: {}", testDTO.getVendorCode());
        return databaseConnectionTestService
            .testConnection(testDTO)
            .map(result -> ResponseEntity.ok().body(result))
            .onErrorResume(throwable -> {
                log.error("Error testing database connection", throwable);
                DatabaseConnectionTestResult errorResult = new DatabaseConnectionTestResult(
                    false,
                    "Connection test failed: " + throwable.getMessage()
                );
                return Mono.just(ResponseEntity.ok().body(errorResult));
            });
    }
}

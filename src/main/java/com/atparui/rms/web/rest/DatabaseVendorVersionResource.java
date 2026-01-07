package com.atparui.rms.web.rest;

import com.atparui.rms.domain.DatabaseVersion;
import com.atparui.rms.service.DatabaseVendorVersionService;
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
@RequestMapping("/api/database-vendor-versions")
public class DatabaseVendorVersionResource {

    private static final Logger log = LoggerFactory.getLogger(DatabaseVendorVersionResource.class);
    private static final String ENTITY_NAME = "databaseVendorVersion";
    private final DatabaseVendorVersionService versionService;

    public DatabaseVendorVersionResource(DatabaseVendorVersionService versionService) {
        this.versionService = versionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<DatabaseVersion>> createVersion(@Valid @RequestBody DatabaseVersion version) throws URISyntaxException {
        if (version.getId() != null) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(
                        HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idexists", "A new version cannot already have an ID")
                    )
                    .build()
            );
        }

        return versionService
            .save(version)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/database-vendor-versions/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            })
            .onErrorResume(throwable -> {
                String errorMessage = throwable.getMessage();
                String errorKey = "versionexists";

                if (errorMessage.contains("already exists")) {
                    errorKey = "versionalreadyexists";
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
    public Mono<ResponseEntity<DatabaseVersion>> updateVersion(@PathVariable Long id, @Valid @RequestBody DatabaseVersion version) {
        if (version.getId() == null || !version.getId().equals(id)) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idinvalid", "Invalid ID"))
                    .build()
            );
        }
        return versionService
            .save(version)
            .map(result ->
                ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                    .body(result)
            )
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping
    public Flux<DatabaseVersion> getAllVersions(@RequestParam(required = false) Long databaseId) {
        if (databaseId != null) {
            return versionService.findByDatabaseId(databaseId);
        }
        return versionService.findAll();
    }

    @GetMapping("/recent")
    public Flux<DatabaseVersion> getRecentVersions(
        @RequestParam(required = false) Long databaseId,
        @RequestParam(required = false) Long vendorId,
        @RequestParam(defaultValue = "3") int years
    ) {
        // Support both databaseId (new) and vendorId (backward compatibility)
        if (databaseId != null) {
            return versionService.findRecentVersions(databaseId, years);
        } else if (vendorId != null) {
            return versionService.findRecentVersionsByVendorId(vendorId, years);
        } else {
            return Flux.error(new IllegalArgumentException("Either databaseId or vendorId must be provided"));
        }
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<DatabaseVersion>> getVersion(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(versionService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> deleteVersion(@PathVariable Long id) {
        return versionService
            .delete(id)
            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert("rmsApp", false, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }
}

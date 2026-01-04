package com.atparui.rms.web.rest;

import com.atparui.rms.domain.DatabaseVendor;
import com.atparui.rms.service.DatabaseVendorService;
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
@RequestMapping("/api/database-vendors")
public class DatabaseVendorResource {

    private static final String ENTITY_NAME = "databaseVendor";
    private final DatabaseVendorService databaseVendorService;

    public DatabaseVendorResource(DatabaseVendorService databaseVendorService) {
        this.databaseVendorService = databaseVendorService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<DatabaseVendor>> createDatabaseVendor(@Valid @RequestBody DatabaseVendor databaseVendor)
        throws URISyntaxException {
        if (databaseVendor.getId() != null) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(
                        HeaderUtil.createFailureAlert(
                            "rmsApp",
                            false,
                            ENTITY_NAME,
                            "idexists",
                            "A new database vendor cannot already have an ID"
                        )
                    )
                    .build()
            );
        }

        return databaseVendorService
            .save(databaseVendor)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/database-vendors/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            })
            .onErrorResume(throwable -> {
                String errorMessage = throwable.getMessage();
                String errorKey = "databasevendorexists";

                if (errorMessage.contains("vendor code already exists")) {
                    errorKey = "vendorcodeexists";
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
    public Mono<ResponseEntity<DatabaseVendor>> updateDatabaseVendor(
        @PathVariable Long id,
        @Valid @RequestBody DatabaseVendor databaseVendor
    ) {
        if (databaseVendor.getId() == null || !databaseVendor.getId().equals(id)) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idinvalid", "Invalid ID"))
                    .build()
            );
        }
        return databaseVendorService
            .save(databaseVendor)
            .map(result ->
                ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                    .body(result)
            )
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping
    public Flux<DatabaseVendor> getAllDatabaseVendors(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return databaseVendorService.findAllActive();
        }
        return databaseVendorService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<DatabaseVendor>> getDatabaseVendor(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(databaseVendorService.findById(id));
    }

    @GetMapping("/code/{vendorCode}")
    public Mono<ResponseEntity<DatabaseVendor>> getDatabaseVendorByCode(@PathVariable String vendorCode) {
        return ResponseUtil.wrapOrNotFound(databaseVendorService.findByVendorCode(vendorCode));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> deleteDatabaseVendor(@PathVariable Long id) {
        return databaseVendorService
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

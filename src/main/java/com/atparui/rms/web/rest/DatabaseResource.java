package com.atparui.rms.web.rest;

import com.atparui.rms.domain.Database;
import com.atparui.rms.service.DatabaseService;
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
@RequestMapping("/api/databases")
public class DatabaseResource {

    private static final Logger log = LoggerFactory.getLogger(DatabaseResource.class);
    private static final String ENTITY_NAME = "database";
    private final DatabaseService databaseService;

    public DatabaseResource(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Database>> createDatabase(@Valid @RequestBody Database database) throws URISyntaxException {
        if (database.getId() != null) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(
                        HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idexists", "A new database cannot already have an ID")
                    )
                    .build()
            );
        }

        return databaseService
            .save(database)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/databases/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            })
            .onErrorResume(throwable -> {
                String errorMessage = throwable.getMessage();
                String errorKey = "databaseexists";

                if (errorMessage.contains("Database code already exists")) {
                    errorKey = "databasecodeexists";
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
    public Mono<ResponseEntity<Database>> updateDatabase(@PathVariable Long id, @Valid @RequestBody Database database) {
        if (database.getId() == null || !database.getId().equals(id)) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idinvalid", "Invalid ID"))
                    .build()
            );
        }
        return databaseService
            .save(database)
            .map(result ->
                ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                    .body(result)
            )
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping
    public Flux<Database> getAllDatabases(@RequestParam(required = false) Long vendorId) {
        if (vendorId != null) {
            return databaseService.findByVendorId(vendorId);
        }
        return databaseService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Database>> getDatabase(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(databaseService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> deleteDatabase(@PathVariable Long id) {
        return databaseService
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

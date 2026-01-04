package com.atparui.rms.web.rest;

import com.atparui.rms.domain.DatabaseDriver;
import com.atparui.rms.service.DatabaseDriverService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

@RestController
@RequestMapping("/api/database-drivers")
public class DatabaseDriverResource {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDriverResource.class);
    private static final String ENTITY_NAME = "databaseDriver";
    private final DatabaseDriverService driverService;

    public DatabaseDriverResource(DatabaseDriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<DatabaseDriver>> uploadDriver(
        @RequestParam("vendorId") Long vendorId,
        @RequestParam("versionId") Long versionId,
        @RequestParam("driverType") String driverType,
        @RequestParam("driverClassName") String driverClassName,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "description", required = false) String description
    ) throws URISyntaxException {
        log.debug(
            "REST request to upload driver: vendorId={}, versionId={}, type={}, file={}",
            vendorId,
            versionId,
            driverType,
            file.getOriginalFilename()
        );

        // Get current user (you may need to inject SecurityContext or similar)
        String uploadedBy = "admin"; // TODO: Get from security context

        return driverService
            .uploadDriver(vendorId, versionId, driverType, driverClassName, file, uploadedBy)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/database-drivers/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            })
            .onErrorResume(throwable -> {
                log.error("Error uploading driver", throwable);
                return Mono.just(
                    ResponseEntity.badRequest()
                        .headers(HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "uploadfailed", throwable.getMessage()))
                        .build()
                );
            });
    }

    @GetMapping
    public Flux<DatabaseDriver> getAllDrivers(
        @RequestParam(required = false) Long vendorId,
        @RequestParam(required = false) Long versionId,
        @RequestParam(required = false) String driverType
    ) {
        if (vendorId != null && versionId != null && driverType != null) {
            return driverService.findByVendorIdAndVersionIdAndDriverType(vendorId, versionId, driverType);
        } else if (vendorId != null && versionId != null) {
            return driverService.findByVendorIdAndVersionId(vendorId, versionId);
        } else if (vendorId != null) {
            return driverService.findByVendorId(vendorId);
        }
        return driverService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<DatabaseDriver>> getDriver(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(driverService.findById(id));
    }

    @GetMapping("/default")
    public Mono<ResponseEntity<DatabaseDriver>> getDefaultDriver(
        @RequestParam Long vendorId,
        @RequestParam Long versionId,
        @RequestParam String driverType
    ) {
        return ResponseUtil.wrapOrNotFound(driverService.findDefaultDriver(vendorId, versionId, driverType));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<DatabaseDriver>> updateDriver(@PathVariable Long id, @Valid @RequestBody DatabaseDriver driver) {
        if (driver.getId() == null || !driver.getId().equals(id)) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("rmsApp", false, ENTITY_NAME, "idinvalid", "Invalid ID"))
                    .build()
            );
        }
        return driverService
            .save(driver)
            .map(result ->
                ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert("rmsApp", false, ENTITY_NAME, result.getId().toString()))
                    .body(result)
            )
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> deleteDriver(@PathVariable Long id) {
        return driverService
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

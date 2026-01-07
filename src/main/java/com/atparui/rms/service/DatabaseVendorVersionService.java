package com.atparui.rms.service;

import com.atparui.rms.domain.DatabaseVersion;
import com.atparui.rms.repository.DatabaseVendorVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DatabaseVendorVersionService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseVendorVersionService.class);

    private final DatabaseVendorVersionRepository versionRepository;

    public DatabaseVendorVersionService(DatabaseVendorVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    public Mono<DatabaseVersion> findById(Long id) {
        return versionRepository.findById(id);
    }

    public Flux<DatabaseVersion> findByDatabaseId(Long databaseId) {
        return versionRepository.findByDatabaseIdAndActiveTrue(databaseId);
    }

    /**
     * Get versions from last N years (default 3).
     *
     * @param databaseId the database ID
     * @param years number of years (default 3)
     * @return Flux of versions
     */
    public Flux<DatabaseVersion> findRecentVersions(Long databaseId, int years) {
        return versionRepository.findRecentVersions(databaseId, years);
    }

    /**
     * Get versions from last N years for all databases of a vendor (default 3).
     * This is for backward compatibility with the old API that used vendorId.
     *
     * @param vendorId the vendor ID
     * @param years number of years (default 3)
     * @return Flux of versions
     */
    public Flux<DatabaseVersion> findRecentVersionsByVendorId(Long vendorId, int years) {
        return versionRepository.findRecentVersionsByVendorId(vendorId, years);
    }

    public Flux<DatabaseVersion> findAll() {
        return versionRepository.findAll();
    }

    public Mono<DatabaseVersion> save(DatabaseVersion version) {
        if (version.getId() == null) {
            // Check if version already exists for this database
            return versionRepository
                .existsByDatabaseIdAndVersion(version.getDatabaseId(), version.getVersion())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Version " + version.getVersion() + " already exists for this vendor"));
                    }
                    return versionRepository.save(version);
                });
        } else {
            return versionRepository.save(version);
        }
    }

    public Mono<Void> delete(Long id) {
        return versionRepository
            .findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Version not found with ID: " + id)))
            .flatMap(version -> {
                log.info("Deleting database vendor version: {} (ID: {})", version.getVersion(), id);
                return versionRepository.deleteById(id);
            })
            .then();
    }
}

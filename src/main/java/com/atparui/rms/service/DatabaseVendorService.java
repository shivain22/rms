package com.atparui.rms.service;

import com.atparui.rms.domain.DatabaseVendor;
import com.atparui.rms.repository.DatabaseVendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DatabaseVendorService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseVendorService.class);

    private final DatabaseVendorRepository databaseVendorRepository;

    public DatabaseVendorService(DatabaseVendorRepository databaseVendorRepository) {
        this.databaseVendorRepository = databaseVendorRepository;
    }

    public Mono<DatabaseVendor> findById(Long id) {
        return databaseVendorRepository.findById(id);
    }

    public Mono<DatabaseVendor> findByVendorCode(String vendorCode) {
        return databaseVendorRepository.findByVendorCodeAndActiveTrue(vendorCode);
    }

    public Flux<DatabaseVendor> findAll() {
        return databaseVendorRepository.findAll();
    }

    public Flux<DatabaseVendor> findAllActive() {
        return databaseVendorRepository.findAllActive();
    }

    public Mono<DatabaseVendor> save(DatabaseVendor databaseVendor) {
        if (databaseVendor.getId() == null) {
            // Check if vendor code already exists
            return databaseVendorRepository
                .existsByVendorCode(databaseVendor.getVendorCode())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Database vendor code already exists: " + databaseVendor.getVendorCode()));
                    }
                    return databaseVendorRepository.save(databaseVendor);
                });
        } else {
            return databaseVendorRepository.save(databaseVendor);
        }
    }

    public Mono<Void> delete(Long id) {
        return databaseVendorRepository
            .findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Database vendor not found with ID: " + id)))
            .flatMap(databaseVendor -> {
                log.info("Deleting database vendor: {} (ID: {})", databaseVendor.getVendorCode(), id);
                return databaseVendorRepository.deleteById(id);
            })
            .then();
    }
}

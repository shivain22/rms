package com.atparui.rms.service;

import com.atparui.rms.domain.Database;
import com.atparui.rms.repository.DatabaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final DatabaseRepository databaseRepository;

    public DatabaseService(DatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    public Mono<Database> findById(Long id) {
        return databaseRepository.findById(id);
    }

    public Flux<Database> findAll() {
        return databaseRepository.findAll();
    }

    public Flux<Database> findAllActive() {
        return databaseRepository.findAllActive();
    }

    public Flux<Database> findByVendorId(Long vendorId) {
        return databaseRepository.findByVendorIdAndActiveTrue(vendorId);
    }

    public Mono<Database> save(Database database) {
        if (database.getId() == null) {
            // Check if database code already exists for this vendor
            return databaseRepository
                .existsByVendorIdAndDatabaseCode(database.getVendorId(), database.getDatabaseCode())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(
                            new RuntimeException("Database code already exists for this vendor: " + database.getDatabaseCode())
                        );
                    }
                    return databaseRepository.save(database);
                });
        } else {
            return databaseRepository.save(database);
        }
    }

    public Mono<Void> delete(Long id) {
        return databaseRepository
            .findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Database not found with ID: " + id)))
            .flatMap(database -> {
                log.info("Deleting database: {} (ID: {})", database.getDatabaseCode(), id);
                return databaseRepository.deleteById(id);
            })
            .then();
    }
}

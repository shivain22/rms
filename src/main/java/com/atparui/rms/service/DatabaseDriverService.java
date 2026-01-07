package com.atparui.rms.service;

import com.atparui.rms.domain.DriverJar;
import com.atparui.rms.repository.DatabaseDriverRepository;
import com.atparui.rms.repository.DatabaseVendorVersionRepository;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DatabaseDriverService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDriverService.class);

    private final DatabaseDriverRepository driverRepository;
    private final DriverStorageService driverStorageService;
    private final DatabaseVendorVersionRepository versionRepository;

    public DatabaseDriverService(
        DatabaseDriverRepository driverRepository,
        DriverStorageService driverStorageService,
        DatabaseVendorVersionRepository versionRepository
    ) {
        this.driverRepository = driverRepository;
        this.driverStorageService = driverStorageService;
        this.versionRepository = versionRepository;
    }

    public Flux<DriverJar> findAll() {
        return driverRepository.findAll();
    }

    public Mono<DriverJar> findById(Long id) {
        return driverRepository.findById(id);
    }

    public Flux<DriverJar> findByVersionId(Long versionId) {
        return driverRepository.findByVersionId(versionId);
    }

    public Flux<DriverJar> findByVersionIdAndDriverType(Long versionId, String driverType) {
        return driverRepository.findByVersionIdAndDriverType(versionId, driverType);
    }

    public Mono<DriverJar> findDefaultDriver(Long versionId, String driverType) {
        return driverRepository.findDefaultDriver(versionId, driverType);
    }

    /**
     * Upload and store a driver JAR file.
     *
     * @param versionId the version ID
     * @param driverType the driver type (JDBC or R2DBC)
     * @param driverClassName the driver class name
     * @param file the JAR file
     * @param uploadedBy the user who uploaded
     * @return Mono containing saved DriverJar
     */
    public Mono<DriverJar> uploadDriver(Long versionId, String driverType, String driverClassName, MultipartFile file, String uploadedBy) {
        return versionRepository
            .findById(versionId)
            .switchIfEmpty(Mono.error(new RuntimeException("Database version not found")))
            .flatMap(version -> {
                try {
                    // Calculate MD5 hash
                    String md5Hash = driverStorageService.calculateMd5Hash(file);

                    // Store file - we need to get database info to build path
                    // For now, use version ID in path
                    String filePath = driverStorageService.storeDriver("version_" + versionId, version.getVersion(), driverType, file);

                    // Get file size
                    long fileSize = driverStorageService.getFileSize(filePath);

                    // Create driver entity
                    DriverJar driver = new DriverJar();
                    driver.setVersionId(versionId);
                    driver.setDriverType(driverType.toUpperCase());
                    driver.setFilePath(filePath);
                    driver.setFileName(file.getOriginalFilename());
                    driver.setFileSize(fileSize);
                    driver.setDriverClassName(driverClassName);
                    driver.setMd5Hash(md5Hash);
                    driver.setUploadedBy(uploadedBy);
                    driver.setActive(true);

                    // Check if this should be default driver
                    return driverRepository
                        .findByVersionIdAndDriverType(versionId, driverType)
                        .hasElements()
                        .flatMap(hasDrivers -> {
                            if (!hasDrivers) {
                                // First driver for this version/type, make it default
                                driver.setIsDefault(true);
                            }
                            return driverRepository.save(driver);
                        });
                } catch (IOException e) {
                    log.error("Error uploading driver file", e);
                    return Mono.error(new RuntimeException("Failed to upload driver file: " + e.getMessage(), e));
                }
            });
    }

    public Mono<DriverJar> save(DriverJar driver) {
        return driverRepository.save(driver);
    }

    public Mono<Void> delete(Long id) {
        return driverRepository
            .findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Driver not found with ID: " + id)))
            .flatMap(driver -> {
                try {
                    // Delete file
                    driverStorageService.deleteDriver(driver.getFilePath());
                } catch (IOException e) {
                    log.warn("Failed to delete driver file: {}", driver.getFilePath(), e);
                }
                log.info("Deleting database driver: {} (ID: {})", driver.getFileName(), id);
                return driverRepository.deleteById(id);
            })
            .then();
    }
}

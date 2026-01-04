package com.atparui.rms.service;

import com.atparui.rms.domain.DatabaseDriver;
import com.atparui.rms.repository.DatabaseDriverRepository;
import com.atparui.rms.repository.DatabaseVendorRepository;
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
    private final DatabaseVendorRepository databaseVendorRepository;
    private final DatabaseVendorVersionRepository versionRepository;

    public DatabaseDriverService(
        DatabaseDriverRepository driverRepository,
        DriverStorageService driverStorageService,
        DatabaseVendorRepository databaseVendorRepository,
        DatabaseVendorVersionRepository versionRepository
    ) {
        this.driverRepository = driverRepository;
        this.driverStorageService = driverStorageService;
        this.databaseVendorRepository = databaseVendorRepository;
        this.versionRepository = versionRepository;
    }

    public Mono<DatabaseDriver> findById(Long id) {
        return driverRepository.findById(id);
    }

    public Flux<DatabaseDriver> findByVendorId(Long vendorId) {
        return driverRepository.findByVendorId(vendorId);
    }

    public Flux<DatabaseDriver> findByVendorIdAndVersionId(Long vendorId, Long versionId) {
        return driverRepository.findByVendorIdAndVersionId(vendorId, versionId);
    }

    public Flux<DatabaseDriver> findByVendorIdAndVersionIdAndDriverType(Long vendorId, Long versionId, String driverType) {
        return driverRepository.findByVendorIdAndVersionIdAndDriverType(vendorId, versionId, driverType);
    }

    public Mono<DatabaseDriver> findDefaultDriver(Long vendorId, Long versionId, String driverType) {
        return driverRepository.findDefaultDriver(vendorId, versionId, driverType);
    }

    /**
     * Upload and store a driver JAR file.
     *
     * @param vendorId the vendor ID
     * @param versionId the version ID
     * @param driverType the driver type (JDBC or R2DBC)
     * @param driverClassName the driver class name
     * @param file the JAR file
     * @param uploadedBy the user who uploaded
     * @return Mono containing saved DatabaseDriver
     */
    public Mono<DatabaseDriver> uploadDriver(
        Long vendorId,
        Long versionId,
        String driverType,
        String driverClassName,
        MultipartFile file,
        String uploadedBy
    ) {
        return Mono.zip(databaseVendorRepository.findById(vendorId), versionRepository.findById(versionId))
            .switchIfEmpty(Mono.error(new RuntimeException("Vendor or version not found")))
            .flatMap(tuple -> {
                com.atparui.rms.domain.DatabaseVendor vendor = tuple.getT1();
                com.atparui.rms.domain.DatabaseVendorVersion version = tuple.getT2();

                try {
                    // Calculate MD5 hash
                    String md5Hash = driverStorageService.calculateMd5Hash(file);

                    // Store file
                    String filePath = driverStorageService.storeDriver(vendor.getVendorCode(), version.getVersion(), driverType, file);

                    // Get file size
                    long fileSize = driverStorageService.getFileSize(filePath);

                    // Create driver entity
                    DatabaseDriver driver = new DatabaseDriver();
                    driver.setVendorId(vendorId);
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
                        .findByVendorIdAndVersionIdAndDriverType(vendorId, versionId, driverType)
                        .hasElements()
                        .flatMap(hasDrivers -> {
                            if (!hasDrivers) {
                                // First driver for this vendor/version/type, make it default
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

    public Mono<DatabaseDriver> save(DatabaseDriver driver) {
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

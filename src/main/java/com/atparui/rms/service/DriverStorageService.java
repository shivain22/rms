package com.atparui.rms.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DriverStorageService {

    private static final Logger log = LoggerFactory.getLogger(DriverStorageService.class);

    @Value("${database.driver.storage.path:./drivers}")
    private String storageBasePath;

    /**
     * Store driver JAR file.
     *
     * @param vendorCode the vendor code
     * @param version the version
     * @param driverType the driver type (JDBC or R2DBC)
     * @param file the uploaded file
     * @return relative path to stored file
     */
    public String storeDriver(String vendorCode, String version, String driverType, MultipartFile file) throws IOException {
        // Create directory structure: drivers/{vendor}/{version}/{type}/
        Path vendorDir = Path.of(storageBasePath, vendorCode.toLowerCase());
        Path versionDir = vendorDir.resolve(version);
        Path typeDir = versionDir.resolve(driverType.toLowerCase());

        // Create directories if they don't exist
        Files.createDirectories(typeDir);

        // Generate filename: {vendor}-{version}-{type}.jar
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            extension = ".jar";
        }

        String filename = String.format("%s-%s-%s%s", vendorCode.toLowerCase(), version, driverType.toLowerCase(), extension);
        Path targetFile = typeDir.resolve(filename);

        // Save file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return relative path from storage base
        Path relativePath = Path.of(storageBasePath).relativize(targetFile);
        log.info("Stored driver JAR: {} -> {}", originalFilename, relativePath);
        return relativePath.toString().replace("\\", "/"); // Normalize path separators
    }

    /**
     * Get driver file as Resource.
     *
     * @param filePath the relative file path
     * @return Resource for the file
     */
    public Resource loadDriverAsResource(String filePath) throws IOException {
        Path file = Path.of(storageBasePath, filePath);
        Resource resource = new UrlResource(file.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Driver file not found or not readable: " + filePath);
        }
    }

    /**
     * Get absolute path to driver file.
     *
     * @param filePath the relative file path
     * @return absolute Path
     */
    public Path getDriverPath(String filePath) {
        return Path.of(storageBasePath, filePath);
    }

    /**
     * Calculate MD5 hash of file.
     *
     * @param file the file
     * @return MD5 hash string
     */
    public String calculateMd5Hash(MultipartFile file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    /**
     * Delete driver file.
     *
     * @param filePath the relative file path
     */
    public void deleteDriver(String filePath) throws IOException {
        Path file = Path.of(storageBasePath, filePath);
        if (Files.exists(file)) {
            Files.delete(file);
            log.info("Deleted driver file: {}", filePath);
        }
    }

    /**
     * Get file size.
     *
     * @param filePath the relative file path
     * @return file size in bytes
     */
    public long getFileSize(String filePath) throws IOException {
        Path file = Path.of(storageBasePath, filePath);
        return Files.size(file);
    }
}

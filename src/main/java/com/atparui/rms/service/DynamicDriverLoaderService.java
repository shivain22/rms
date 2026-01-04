package com.atparui.rms.service;

import com.atparui.rms.domain.DatabaseDriver;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DynamicDriverLoaderService {

    private static final Logger log = LoggerFactory.getLogger(DynamicDriverLoaderService.class);

    private final DriverStorageService driverStorageService;
    private final Map<String, ClassLoader> driverClassLoaders = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> loadedDriverClasses = new ConcurrentHashMap<>();

    public DynamicDriverLoaderService(DriverStorageService driverStorageService) {
        this.driverStorageService = driverStorageService;
    }

    /**
     * Load driver class from JAR file.
     *
     * @param driver the database driver entity
     * @return the driver class
     */
    public Class<?> loadDriverClass(DatabaseDriver driver) throws ClassNotFoundException, IOException {
        String cacheKey = driver.getId() + "_" + driver.getDriverClassName();

        // Check cache
        if (loadedDriverClasses.containsKey(cacheKey)) {
            log.debug("Using cached driver class: {}", driver.getDriverClassName());
            return loadedDriverClasses.get(cacheKey);
        }

        // Get file path
        Path driverPath = driverStorageService.getDriverPath(driver.getFilePath());
        File driverFile = driverPath.toFile();

        if (!driverFile.exists()) {
            throw new IOException("Driver file not found: " + driver.getFilePath());
        }

        // Create URL for the JAR file
        URL jarUrl = driverFile.toURI().toURL();
        log.debug("Loading driver from: {}", jarUrl);

        // Create or get classloader for this driver
        ClassLoader classLoader = driverClassLoaders.computeIfAbsent(driver.getFilePath(), path ->
            new URLClassLoader(new URL[] { jarUrl }, Thread.currentThread().getContextClassLoader())
        );

        // Load the driver class
        String driverClassName = driver.getDriverClassName();
        if (driverClassName == null || driverClassName.isEmpty()) {
            throw new IllegalArgumentException("Driver class name is required");
        }

        Class<?> driverClass = classLoader.loadClass(driverClassName);
        loadedDriverClasses.put(cacheKey, driverClass);

        log.info("Successfully loaded driver class: {} from {}", driverClassName, driver.getFileName());
        return driverClass;
    }

    /**
     * Load and register JDBC driver.
     *
     * @param driver the database driver entity
     * @return the loaded driver instance
     */
    public java.sql.Driver loadJdbcDriver(DatabaseDriver driver) throws Exception {
        Class<?> driverClass = loadDriverClass(driver);

        // Verify it's a JDBC Driver
        if (!java.sql.Driver.class.isAssignableFrom(driverClass)) {
            throw new ClassCastException("Class " + driver.getDriverClassName() + " is not a JDBC Driver");
        }

        // Instantiate the driver
        java.sql.Driver driverInstance = (java.sql.Driver) driverClass.getDeclaredConstructor().newInstance();

        log.info("Successfully loaded and instantiated JDBC driver: {}", driver.getDriverClassName());
        return driverInstance;
    }

    /**
     * Clear cached classloader for a driver (useful when driver is updated).
     *
     * @param filePath the driver file path
     */
    public void clearDriverCache(String filePath) {
        driverClassLoaders.remove(filePath);
        // Remove all entries for this driver
        loadedDriverClasses.entrySet().removeIf(entry -> entry.getKey().startsWith(filePath));
        log.debug("Cleared cache for driver: {}", filePath);
    }

    /**
     * Clear all cached drivers.
     */
    public void clearAllCache() {
        driverClassLoaders.clear();
        loadedDriverClasses.clear();
        log.info("Cleared all driver caches");
    }
}

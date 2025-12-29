package com.atparui.rms.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TenantLiquibaseService {

    private static final Logger log = LoggerFactory.getLogger(TenantLiquibaseService.class);

    @Value("${tenant.liquibase.repository.url:https://github.com/shivain22/rms-service.git}")
    private String repositoryUrl;

    @Value("${tenant.liquibase.repository.branch:master}")
    private String repositoryBranch;

    @Value("${tenant.liquibase.repository.local-path:${java.io.tmpdir}/rms-service-repo}")
    private String localRepositoryPath;

    @Value("${tenant.liquibase.changelog-path:config/liquibase/master.xml}")
    private String changelogPath;

    /**
     * Clone or pull the rms-service repository to get the latest Liquibase changelogs.
     *
     * @return the path to the local repository
     * @throws Exception if cloning or pulling fails
     */
    public Path ensureRepositoryCloned() throws Exception {
        Path repoPath = Paths.get(localRepositoryPath);

        if (Files.exists(repoPath) && Files.exists(repoPath.resolve(".git"))) {
            // Repository exists, pull latest changes
            log.info("Repository already exists at {}, pulling latest changes...", repoPath);
            try (Git git = Git.open(repoPath.toFile())) {
                PullCommand pull = git.pull();
                pull.setRemoteBranchName(repositoryBranch);
                pull.call();
                log.info("Successfully pulled latest changes from repository");
            } catch (GitAPIException e) {
                log.error("Failed to pull repository: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to pull repository", e);
            }
        } else {
            // Clone the repository
            log.info("Cloning repository from {} to {}...", repositoryUrl, repoPath);
            try {
                Files.createDirectories(repoPath.getParent());
                Git.cloneRepository().setURI(repositoryUrl).setDirectory(repoPath.toFile()).setBranch(repositoryBranch).call();
                log.info("Successfully cloned repository to {}", repoPath);
            } catch (GitAPIException e) {
                log.error("Failed to clone repository: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to clone repository", e);
            }
        }

        return repoPath;
    }

    /**
     * Apply Liquibase changes to a tenant database.
     *
     * @param tenantId the tenant ID
     * @param databaseUrl the JDBC database URL
     * @param username the database username
     * @param password the database password
     * @throws Exception if applying changes fails
     */
    public void applyLiquibaseChanges(String tenantId, String databaseUrl, String username, String password) throws Exception {
        log.info("Applying Liquibase changes for tenant: {}", tenantId);

        // Ensure repository is cloned/pulled
        Path repoPath = ensureRepositoryCloned();

        // Convert R2DBC URL to JDBC if needed
        String jdbcUrl = convertToJdbcUrl(databaseUrl);

        // Path to the src/main/resources directory in the repository
        // This is where the config/liquibase directory is located
        Path resourcesPath = repoPath.resolve("src/main/resources");

        // Path to the changelog file relative to resourcesPath
        Path changelogFile = resourcesPath.resolve(changelogPath);

        if (!Files.exists(changelogFile)) {
            throw new RuntimeException("Changelog file not found: " + changelogFile);
        }

        log.info("Using changelog file: {}", changelogFile);

        // Apply Liquibase changes
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

            // Use CompositeResourceAccessor with FileSystemResourceAccessor
            // Set the root to src/main/resources so that paths in master.xml (like config/liquibase/changelog/...)
            // resolve correctly relative to the resource accessor root
            // Note: FileSystemResourceAccessor is deprecated but still functional
            @SuppressWarnings("deprecation")
            FileSystemResourceAccessor fileSystemAccessor = new FileSystemResourceAccessor(resourcesPath.toFile());
            CompositeResourceAccessor resourceAccessor = new CompositeResourceAccessor(fileSystemAccessor);

            try (Liquibase liquibase = new Liquibase(changelogPath, resourceAccessor, database)) {
                log.info("Running Liquibase update for tenant: {}", tenantId);
                liquibase.update("");

                log.info("Successfully applied Liquibase changes for tenant: {}", tenantId);
            }
        } catch (LiquibaseException | java.sql.SQLException e) {
            log.error("Failed to apply Liquibase changes for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to apply Liquibase changes", e);
        }
    }

    /**
     * Convert R2DBC URL to JDBC URL format.
     *
     * @param url the database URL (can be R2DBC or JDBC format)
     * @return JDBC URL format
     */
    private String convertToJdbcUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Database URL cannot be null");
        }

        // If already JDBC format, return as is
        if (url.startsWith("jdbc:")) {
            return url;
        }

        // Convert R2DBC to JDBC
        if (url.startsWith("r2dbc:")) {
            return url.replace("r2dbc:", "jdbc:");
        }

        // If no prefix, assume it needs jdbc:postgresql:// prefix
        if (!url.contains("://")) {
            return "jdbc:postgresql://" + url;
        }

        // Default: prepend jdbc:
        return "jdbc:" + url.substring(url.indexOf("://"));
    }
}

package com.atparui.rms.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("platforms")
public class Platform {

    @Id
    private Long id;

    @NotNull
    @Size(min = 2, max = 100)
    @Column("name")
    private String name;

    @NotNull
    @Size(min = 2, max = 10)
    @Column("prefix")
    private String prefix;

    @Size(max = 500)
    @Column("description")
    private String description;

    @Size(max = 100)
    @Column("subdomain")
    private String subdomain;

    @Size(max = 255)
    @Column("webapp_github_repo")
    private String webappGithubRepo;

    @Size(max = 255)
    @Column("mobile_github_repo")
    private String mobileGithubRepo;

    // Platform-specific database configuration (for future per-platform PostgreSQL)
    // If null, uses default shared PostgreSQL from application config
    @Size(max = 255)
    @Column("database_host")
    private String databaseHost;

    @Column("database_port")
    private Integer databasePort;

    @Size(max = 100)
    @Column("database_admin_username")
    private String databaseAdminUsername;

    @Size(max = 255)
    @Column("database_admin_password")
    private String databaseAdminPassword;

    @Size(max = 100)
    @Column("database_name")
    private String databaseName;

    // Indicates if template and default databases have been initialized
    @Column("database_initialized")
    private Boolean databaseInitialized = false;

    private Boolean active = true;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Constructors
    public Platform() {}

    public Platform(String name, String prefix, String description) {
        this.name = name;
        this.prefix = prefix;
        this.description = description;
        this.active = true;
        this.databaseInitialized = false;
    }

    public Platform(String name, String prefix, String description, String subdomain, String webappGithubRepo, String mobileGithubRepo) {
        this.name = name;
        this.prefix = prefix;
        this.description = description;
        this.subdomain = subdomain;
        this.webappGithubRepo = webappGithubRepo;
        this.mobileGithubRepo = mobileGithubRepo;
        this.active = true;
        this.databaseInitialized = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getWebappGithubRepo() {
        return webappGithubRepo;
    }

    public void setWebappGithubRepo(String webappGithubRepo) {
        this.webappGithubRepo = webappGithubRepo;
    }

    public String getMobileGithubRepo() {
        return mobileGithubRepo;
    }

    public void setMobileGithubRepo(String mobileGithubRepo) {
        this.mobileGithubRepo = mobileGithubRepo;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    public Integer getDatabasePort() {
        return databasePort;
    }

    public void setDatabasePort(Integer databasePort) {
        this.databasePort = databasePort;
    }

    public String getDatabaseAdminUsername() {
        return databaseAdminUsername;
    }

    public void setDatabaseAdminUsername(String databaseAdminUsername) {
        this.databaseAdminUsername = databaseAdminUsername;
    }

    public String getDatabaseAdminPassword() {
        return databaseAdminPassword;
    }

    public void setDatabaseAdminPassword(String databaseAdminPassword) {
        this.databaseAdminPassword = databaseAdminPassword;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public Boolean getDatabaseInitialized() {
        return databaseInitialized;
    }

    public void setDatabaseInitialized(Boolean databaseInitialized) {
        this.databaseInitialized = databaseInitialized;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Platform)) return false;
        return id != null && id.equals(((Platform) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Platform{" +
            "id=" +
            id +
            ", name='" +
            name +
            '\'' +
            ", prefix='" +
            prefix +
            '\'' +
            ", subdomain='" +
            subdomain +
            '\'' +
            ", active=" +
            active +
            '}'
        );
    }
}

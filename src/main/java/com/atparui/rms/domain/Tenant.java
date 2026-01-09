package com.atparui.rms.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("tenants")
public class Tenant {

    @Id
    private Long id;

    @NotNull
    @Size(min = 2, max = 50)
    @Column("tenant_key")
    private String tenantKey;

    @Column("tenant_id")
    private String tenantId;

    @NotNull
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 100)
    private String subdomain;

    @Column("database_vendor_code")
    private String databaseVendorCode;

    @Column("database_version_id")
    private Long databaseVersionId;

    @Column("driver_jar_id")
    private Long driverJarId;

    @Column("driver_type")
    private String driverType; // JDBC or R2DBC

    @Column("database_provisioning_mode")
    private String databaseProvisioningMode; // AUTO_CREATE or USE_EXISTING

    @Column("database_ownership_type")
    private String databaseOwnershipType; // PLATFORM or BYOD (Bring Your Own Database)

    @Column("database_host")
    private String databaseHost;

    @Column("database_port")
    private Integer databasePort;

    @Column("database_name")
    private String databaseName;

    @NotNull
    @Column("database_url")
    private String databaseUrl;

    @NotNull
    @Column("database_username")
    private String databaseUsername;

    @NotNull
    @Column("database_password")
    private String databasePassword;

    @NotNull
    @Column("schema_name")
    private String schemaName;

    @Column("realm_name")
    private String realmName;

    @Column("client_id")
    private String clientId;

    @Column("client_secret")
    private String clientSecret;

    @Column("rms_service_client_id")
    private String rmsServiceClientId;

    @Column("rms_service_client_secret")
    private String rmsServiceClientSecret;

    @Column("default_roles")
    private String defaultRoles;

    @Column("platform_id")
    private Long platformId;

    @Column("is_template")
    private Boolean isTemplate = false;

    private Boolean active = true;

    // Transient fields - NOT stored in database
    // Used during BYOD_CREATE mode to create database on external server
    @org.springframework.data.annotation.Transient
    private String adminUsername;

    @org.springframework.data.annotation.Transient
    private String adminPassword;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Constructors
    public Tenant() {}

    public Tenant(
        String tenantKey,
        String tenantId,
        String name,
        String subdomain,
        String databaseUrl,
        String databaseUsername,
        String databasePassword,
        String schemaName
    ) {
        this.tenantKey = tenantKey;
        this.tenantId = tenantId;
        this.name = name;
        this.subdomain = subdomain;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.schemaName = schemaName;
        this.realmName = tenantId + "-realm";
        this.clientId = tenantId + "-web";
        this.clientSecret = generateClientSecret();
        this.defaultRoles = "ROLE_USER,ROLE_ADMIN,ROLE_CUSTOMER";
        this.active = true;
    }

    private String generateClientSecret() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public void setTenantKey(String tenantKey) {
        this.tenantKey = tenantKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getDatabaseVendorCode() {
        return databaseVendorCode;
    }

    public void setDatabaseVendorCode(String databaseVendorCode) {
        this.databaseVendorCode = databaseVendorCode;
    }

    public Long getDatabaseVersionId() {
        return databaseVersionId;
    }

    public void setDatabaseVersionId(Long databaseVersionId) {
        this.databaseVersionId = databaseVersionId;
    }

    public Long getDriverJarId() {
        return driverJarId;
    }

    public void setDriverJarId(Long driverJarId) {
        this.driverJarId = driverJarId;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getDatabaseProvisioningMode() {
        return databaseProvisioningMode;
    }

    public void setDatabaseProvisioningMode(String databaseProvisioningMode) {
        this.databaseProvisioningMode = databaseProvisioningMode;
    }

    public String getDatabaseOwnershipType() {
        return databaseOwnershipType;
    }

    public void setDatabaseOwnershipType(String databaseOwnershipType) {
        this.databaseOwnershipType = databaseOwnershipType;
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

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRmsServiceClientId() {
        return rmsServiceClientId;
    }

    public void setRmsServiceClientId(String rmsServiceClientId) {
        this.rmsServiceClientId = rmsServiceClientId;
    }

    public String getRmsServiceClientSecret() {
        return rmsServiceClientSecret;
    }

    public void setRmsServiceClientSecret(String rmsServiceClientSecret) {
        this.rmsServiceClientSecret = rmsServiceClientSecret;
    }

    public String getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(String defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public Long getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Long platformId) {
        this.platformId = platformId;
    }

    public Boolean getIsTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(Boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Transient getters/setters for admin credentials (BYOD_CREATE mode)
    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
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
        if (!(o instanceof Tenant)) return false;
        return id != null && id.equals(((Tenant) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Tenant{" +
            "id=" +
            id +
            ", tenantKey='" +
            tenantKey +
            '\'' +
            ", tenantId='" +
            tenantId +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", active=" +
            active +
            '}'
        );
    }
}

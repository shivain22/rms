package com.atparui.rms.service.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for testing database connection before tenant creation.
 */
public class DatabaseConnectionTestDTO {

    @NotNull
    private String vendorCode;

    private Long versionId; // Optional: specific version

    private Long driverId; // Optional: specific driver to use

    @NotNull
    private String host;

    @NotNull
    private Integer port;

    @NotNull
    private String databaseName;

    private String schemaName;

    @NotNull
    private String username;

    @NotNull
    private String password;

    public DatabaseConnectionTestDTO() {}

    public DatabaseConnectionTestDTO(
        String vendorCode,
        String host,
        Integer port,
        String databaseName,
        String schemaName,
        String username,
        String password
    ) {
        this.vendorCode = vendorCode;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.schemaName = schemaName;
        this.username = username;
        this.password = password;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

package com.atparui.rms.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * DTO for tenant database configuration.
 * This is returned by the Gateway API endpoint for services to retrieve database connection information.
 */
public class TenantDatabaseConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("databaseUrl")
    private String databaseUrl;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("maxPoolSize")
    private Integer maxPoolSize = 20;

    @JsonProperty("connectionTimeout")
    private Integer connectionTimeout = 30000;

    @JsonProperty("validationQuery")
    private String validationQuery = "SELECT 1";

    @JsonProperty("clients")
    private java.util.List<TenantClientDTO> clients;

    public TenantDatabaseConfigDTO() {
        // Empty constructor needed for Jackson
    }

    public TenantDatabaseConfigDTO(
        String tenantId,
        String databaseUrl,
        String username,
        String password,
        Integer maxPoolSize,
        Integer connectionTimeout,
        String validationQuery
    ) {
        this.tenantId = tenantId;
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize != null ? maxPoolSize : 20;
        this.connectionTimeout = connectionTimeout != null ? connectionTimeout : 30000;
        this.validationQuery = validationQuery != null ? validationQuery : "SELECT 1";
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
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

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public java.util.List<TenantClientDTO> getClients() {
        return clients;
    }

    public void setClients(java.util.List<TenantClientDTO> clients) {
        this.clients = clients;
    }

    @Override
    public String toString() {
        return (
            "TenantDatabaseConfigDTO{" +
            "tenantId='" +
            tenantId +
            '\'' +
            ", databaseUrl='" +
            databaseUrl +
            '\'' +
            ", username='" +
            username +
            '\'' +
            ", maxPoolSize=" +
            maxPoolSize +
            ", connectionTimeout=" +
            connectionTimeout +
            ", validationQuery='" +
            validationQuery +
            '\'' +
            '}'
        );
    }
}

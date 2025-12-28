package com.atparui.rms.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("tenant_clients")
public class TenantClient {

    @Id
    private Long id;

    @NotNull
    @Column("tenant_id")
    private Long tenantId;

    @NotNull
    @Size(min = 1, max = 100)
    @Column("client_id")
    private String clientId;

    @Size(max = 255)
    @Column("client_secret")
    private String clientSecret;

    @NotNull
    @Size(min = 1, max = 50)
    @Column("client_type")
    private String clientType; // web, mobile, rms-service

    @Size(max = 100)
    @Column("realm_name")
    private String realmName;

    @Column("enabled")
    private Boolean enabled = true;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Constructors
    public TenantClient() {}

    public TenantClient(Long tenantId, String clientId, String clientSecret, String clientType, String realmName) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientType = clientType;
        this.realmName = realmName;
        this.enabled = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
        if (!(o instanceof TenantClient)) return false;
        return id != null && id.equals(((TenantClient) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "TenantClient{" +
            "id=" +
            id +
            ", tenantId=" +
            tenantId +
            ", clientId='" +
            clientId +
            '\'' +
            ", clientType='" +
            clientType +
            '\'' +
            ", enabled=" +
            enabled +
            '}'
        );
    }
}

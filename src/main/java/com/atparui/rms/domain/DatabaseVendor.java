package com.atparui.rms.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("database_vendors")
public class DatabaseVendor {

    @Id
    private Long id;

    @NotNull
    @Size(min = 2, max = 50)
    @Column("vendor_code")
    private String vendorCode;

    @NotNull
    @Size(min = 2, max = 100)
    @Column("display_name")
    private String displayName;

    @NotNull
    @Column("default_port")
    private Integer defaultPort;

    @NotNull
    @Size(min = 2, max = 50)
    @Column("driver_key")
    private String driverKey;

    @Size(max = 500)
    @Column("description")
    private String description;

    @Column("jdbc_url_template")
    private String jdbcUrlTemplate;

    @Column("r2dbc_url_template")
    private String r2dbcUrlTemplate;

    @Column("driver_class_name")
    private String driverClassName;

    private Boolean active = true;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Constructors
    public DatabaseVendor() {}

    public DatabaseVendor(String vendorCode, String displayName, Integer defaultPort, String driverKey, String description) {
        this.vendorCode = vendorCode;
        this.displayName = displayName;
        this.defaultPort = defaultPort;
        this.driverKey = driverKey;
        this.description = description;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(Integer defaultPort) {
        this.defaultPort = defaultPort;
    }

    public String getDriverKey() {
        return driverKey;
    }

    public void setDriverKey(String driverKey) {
        this.driverKey = driverKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJdbcUrlTemplate() {
        return jdbcUrlTemplate;
    }

    public void setJdbcUrlTemplate(String jdbcUrlTemplate) {
        this.jdbcUrlTemplate = jdbcUrlTemplate;
    }

    public String getR2dbcUrlTemplate() {
        return r2dbcUrlTemplate;
    }

    public void setR2dbcUrlTemplate(String r2dbcUrlTemplate) {
        this.r2dbcUrlTemplate = r2dbcUrlTemplate;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
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
        if (!(o instanceof DatabaseVendor)) return false;
        return id != null && id.equals(((DatabaseVendor) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "DatabaseVendor{" +
            "id=" +
            id +
            ", vendorCode='" +
            vendorCode +
            '\'' +
            ", displayName='" +
            displayName +
            '\'' +
            ", defaultPort=" +
            defaultPort +
            ", active=" +
            active +
            '}'
        );
    }
}

package com.atparui.rms.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Database entity representing a specific database product from a vendor.
 * For example: Oracle Database, MySQL, PostgreSQL, etc.
 * A vendor can have multiple databases (e.g., Oracle has Oracle Database, Oracle MySQL, etc.)
 */
@Table("databases")
public class Database {

    @Id
    private Long id;

    @NotNull
    @Column("vendor_id")
    private Long vendorId;

    @NotNull
    @Size(min = 2, max = 50)
    @Column("database_code")
    private String databaseCode;

    @NotNull
    @Size(min = 2, max = 100)
    @Column("display_name")
    private String displayName;

    @Size(max = 500)
    @Column("description")
    private String description;

    @Size(max = 200)
    @Column("default_driver_class_name")
    private String defaultDriverClassName;

    @Column("default_port")
    private Integer defaultPort;

    @Size(max = 500)
    @Column("jdbc_url_template")
    private String jdbcUrlTemplate;

    @Size(max = 500)
    @Column("r2dbc_url_template")
    private String r2dbcUrlTemplate;

    private Boolean active = true;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Constructors
    public Database() {}

    public Database(Long vendorId, String databaseCode, String displayName, String description) {
        this.vendorId = vendorId;
        this.databaseCode = databaseCode;
        this.displayName = displayName;
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

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getDatabaseCode() {
        return databaseCode;
    }

    public void setDatabaseCode(String databaseCode) {
        this.databaseCode = databaseCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultDriverClassName() {
        return defaultDriverClassName;
    }

    public void setDefaultDriverClassName(String defaultDriverClassName) {
        this.defaultDriverClassName = defaultDriverClassName;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(Integer defaultPort) {
        this.defaultPort = defaultPort;
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
        if (!(o instanceof Database)) return false;
        return id != null && id.equals(((Database) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "Database{" +
            "id=" +
            id +
            ", vendorId=" +
            vendorId +
            ", databaseCode='" +
            databaseCode +
            '\'' +
            ", displayName='" +
            displayName +
            '\'' +
            ", active=" +
            active +
            '}'
        );
    }
}

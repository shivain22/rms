package com.atparui.rms.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("database_drivers")
public class DatabaseDriver {

    @Id
    private Long id;

    @NotNull
    @Column("vendor_id")
    private Long vendorId;

    @Column("version_id")
    private Long versionId;

    @NotNull
    @Size(min = 1, max = 20)
    @Column("driver_type")
    private String driverType; // JDBC or R2DBC

    @NotNull
    @Size(min = 1, max = 500)
    @Column("file_path")
    private String filePath; // Relative path to stored JAR file

    @NotNull
    @Size(min = 1, max = 200)
    @Column("file_name")
    private String fileName; // Original filename

    @Column("file_size")
    private Long fileSize; // File size in bytes

    @Size(max = 200)
    @Column("driver_class_name")
    private String driverClassName; // Main driver class name

    @Size(max = 100)
    @Column("md5_hash")
    private String md5Hash; // MD5 hash for integrity check

    @Size(max = 1000)
    @Column("description")
    private String description;

    @Column("is_default")
    private Boolean isDefault = false;

    @Column("uploaded_by")
    private String uploadedBy; // User who uploaded

    private Boolean active = true;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Constructors
    public DatabaseDriver() {}

    public DatabaseDriver(Long vendorId, Long versionId, String driverType, String filePath, String fileName) {
        this.vendorId = vendorId;
        this.versionId = versionId;
        this.driverType = driverType;
        this.filePath = filePath;
        this.fileName = fileName;
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

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
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
        if (!(o instanceof DatabaseDriver)) return false;
        return id != null && id.equals(((DatabaseDriver) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "DatabaseDriver{" +
            "id=" +
            id +
            ", vendorId=" +
            vendorId +
            ", versionId=" +
            versionId +
            ", driverType='" +
            driverType +
            '\'' +
            ", fileName='" +
            fileName +
            '\'' +
            ", active=" +
            active +
            '}'
        );
    }
}

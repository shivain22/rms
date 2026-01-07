package com.atparui.rms.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("database_versions")
public class DatabaseVersion {

    @Id
    private Long id;

    @NotNull
    @Column("database_id")
    private Long databaseId;

    @NotNull
    @Size(min = 1, max = 50)
    @Column("version")
    private String version;

    @Size(max = 200)
    @Column("display_name")
    private String displayName;

    @Column("release_date")
    private LocalDate releaseDate;

    @Column("end_of_life_date")
    private LocalDate endOfLifeDate;

    @Size(max = 1000)
    @Column("release_notes")
    private String releaseNotes;

    @Column("is_supported")
    private Boolean isSupported = true;

    @Column("is_recommended")
    private Boolean isRecommended = false;

    private Boolean active = true;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    // Constructors
    public DatabaseVersion() {}

    public DatabaseVersion(Long databaseId, String version, String displayName) {
        this.databaseId = databaseId;
        this.version = version;
        this.displayName = displayName;
        this.isSupported = true;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public LocalDate getEndOfLifeDate() {
        return endOfLifeDate;
    }

    public void setEndOfLifeDate(LocalDate endOfLifeDate) {
        this.endOfLifeDate = endOfLifeDate;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public Boolean getIsSupported() {
        return isSupported;
    }

    public void setIsSupported(Boolean isSupported) {
        this.isSupported = isSupported;
    }

    public Boolean getIsRecommended() {
        return isRecommended;
    }

    public void setIsRecommended(Boolean isRecommended) {
        this.isRecommended = isRecommended;
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
        if (!(o instanceof DatabaseVersion)) return false;
        return id != null && id.equals(((DatabaseVersion) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "DatabaseVersion{" +
            "id=" +
            id +
            ", databaseId=" +
            databaseId +
            ", version='" +
            version +
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

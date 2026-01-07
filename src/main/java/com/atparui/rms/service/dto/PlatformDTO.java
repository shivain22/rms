package com.atparui.rms.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for Platform entity.
 */
public class PlatformDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("createdDate")
    private Instant createdDate;

    @JsonProperty("lastModifiedDate")
    private Instant lastModifiedDate;

    public PlatformDTO() {
        // Empty constructor needed for Jackson
    }

    public PlatformDTO(Long id, String name, String description, Boolean active, Instant createdDate, Instant lastModifiedDate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public String toString() {
        return ("PlatformDTO{" + "id=" + id + ", name='" + name + '\'' + ", active=" + active + '}');
    }
}

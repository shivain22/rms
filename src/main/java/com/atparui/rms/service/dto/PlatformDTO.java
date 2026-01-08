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

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("description")
    private String description;

    @JsonProperty("subdomain")
    private String subdomain;

    @JsonProperty("webappGithubRepo")
    private String webappGithubRepo;

    @JsonProperty("mobileGithubRepo")
    private String mobileGithubRepo;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("createdDate")
    private Instant createdDate;

    @JsonProperty("lastModifiedDate")
    private Instant lastModifiedDate;

    public PlatformDTO() {
        // Empty constructor needed for Jackson
    }

    public PlatformDTO(
        Long id,
        String name,
        String prefix,
        String description,
        String subdomain,
        String webappGithubRepo,
        String mobileGithubRepo,
        Boolean active,
        Instant createdDate,
        Instant lastModifiedDate
    ) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.description = description;
        this.subdomain = subdomain;
        this.webappGithubRepo = webappGithubRepo;
        this.mobileGithubRepo = mobileGithubRepo;
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
        return (
            "PlatformDTO{" +
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

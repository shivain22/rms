package com.atparui.rms.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * DTO for tenant client information.
 */
public class TenantClientDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("clientSecret")
    private String clientSecret;

    @JsonProperty("clientType")
    private String clientType; // web, mobile, rms-service

    public TenantClientDTO() {
        // Empty constructor needed for Jackson
    }

    public TenantClientDTO(String clientId, String clientSecret, String clientType) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientType = clientType;
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

    @Override
    public String toString() {
        return ("TenantClientDTO{" + "clientId='" + clientId + '\'' + ", clientType='" + clientType + '\'' + '}');
    }
}

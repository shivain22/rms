package com.atparui.rms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.admin")
public class GatewayAdminConfig {

    private String realm = "gateway";
    private String clientId = "gateway-web";
    private String clientSecret = "M5nP8qR2sT6uV9wX1yZ3aC4dE7fG0h";
    private String issuerUri = "${multitenancy.keycloak.base-url:https://rmsauth.atparui.com}/realms/gateway";

    // Admin client for realm management
    private String adminClientId = "gateway-admin-client";
    private String adminClientSecret = "K8mN2pQ7rT5vW9xZ1aB3cD4eF6gH8j";

    // Getters and setters
    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }
}

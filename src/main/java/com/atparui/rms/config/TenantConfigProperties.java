package com.atparui.rms.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "tenants")
public class TenantConfigProperties {

    private Map<String, TenantConfig> tenants;

    public Map<String, TenantConfig> getTenants() {
        return tenants;
    }

    public void setTenants(Map<String, TenantConfig> tenants) {
        this.tenants = tenants;
    }

    public TenantConfig getTenantConfig(String tenantId) {
        if (tenants == null) {
            return null;
        }
        return tenants.get(tenantId);
    }

    public static class TenantConfig {

        private String clientId;
        private String clientSecret;
        private DatabaseConfig database;

        // Getters and setters
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

        public DatabaseConfig getDatabase() {
            return database;
        }

        public void setDatabase(DatabaseConfig database) {
            this.database = database;
        }
    }

    public static class DatabaseConfig {

        private String name;
        private String schema;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }
    }
}

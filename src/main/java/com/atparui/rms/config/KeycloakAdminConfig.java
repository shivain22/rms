package com.atparui.rms.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    private final KeycloakAdminProperties properties;

    public KeycloakAdminConfig(KeycloakAdminProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
            .serverUrl(properties.getServerUrl())
            .realm(properties.getRealm())
            .clientId(properties.getClientId())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }
}

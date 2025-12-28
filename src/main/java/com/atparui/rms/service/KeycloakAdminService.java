package com.atparui.rms.service;

import java.util.List;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final Keycloak keycloakAdmin;

    public KeycloakAdminService(Keycloak keycloakAdmin) {
        this.keycloakAdmin = keycloakAdmin;
    }

    /**
     * Get all realms
     */
    public List<RealmRepresentation> getAllRealms() {
        return keycloakAdmin.realms().findAll();
    }

    /**
     * Get realm by name
     */
    public RealmRepresentation getRealm(String realmName) {
        try {
            return keycloakAdmin.realm(realmName).toRepresentation();
        } catch (Exception e) {
            log.error("Realm not found: {}", realmName, e);
            return null;
        }
    }

    /**
     * Get all clients in a realm
     */
    public List<ClientRepresentation> getRealmClients(String realmName) {
        return keycloakAdmin.realm(realmName).clients().findAll();
    }

    /**
     * Get all roles in a realm
     */
    public List<RoleRepresentation> getRealmRoles(String realmName) {
        return keycloakAdmin.realm(realmName).roles().list();
    }

    /**
     * Get all users in a realm
     */
    public List<UserRepresentation> getRealmUsers(String realmName) {
        return keycloakAdmin.realm(realmName).users().list();
    }

    /**
     * Check if realm exists
     */
    public boolean realmExists(String realmName) {
        try {
            keycloakAdmin.realm(realmName).toRepresentation();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get client by client ID in a realm
     */
    public ClientRepresentation getClientByClientId(String realmName, String clientId) {
        return keycloakAdmin.realm(realmName).clients().findByClientId(clientId).stream().findFirst().orElse(null);
    }

    /**
     * Test connection to Keycloak
     */
    public boolean testConnection() {
        try {
            keycloakAdmin.serverInfo().getInfo();
            log.info("Successfully connected to Keycloak admin API");
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to Keycloak admin API", e);
            return false;
        }
    }
}

package com.atparui.rms.service;

import com.atparui.rms.config.RestaurantKeycloakProperties;
import java.util.Arrays;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KeycloakRealmService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakRealmService.class);

    private final Keycloak keycloakAdmin;
    private final RestaurantKeycloakProperties restaurantProperties;

    public KeycloakRealmService(Keycloak keycloakAdmin, RestaurantKeycloakProperties restaurantProperties) {
        this.keycloakAdmin = keycloakAdmin;
        this.restaurantProperties = restaurantProperties;
    }

    public void createTenantRealm(String tenantId, String tenantName) {
        try {
            String realmName = tenantId + "_realm";

            // Create realm
            RealmRepresentation realm = createRealmRepresentation(realmName, tenantName);
            keycloakAdmin.realms().create(realm);
            log.info("Created realm: {}", realmName);

            // Get realm resource for further configuration
            RealmResource realmResource = keycloakAdmin.realm(realmName);

            // Create client scopes
            createClientScopes(realmResource);

            // Create web and mobile clients
            createTenantClient(realmResource, tenantId, "web");
            createTenantClient(realmResource, tenantId, "mobile");

            // Create realm roles
            createRealmRoles(realmResource);

            // Update realm theme to rms-auth-theme-plugin
            updateRealmTheme(realmResource);

            // Copy browser flow and modify it with phone auto-reg form
            KeycloakFlowService flowService = new KeycloakFlowService(keycloakAdmin);
            flowService.copyAndModifyBrowserFlow(realmName);

            log.info("Successfully configured tenant realm: {}", realmName);
        } catch (Exception e) {
            log.error("Failed to create tenant realm for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to create Keycloak realm for tenant: " + tenantId, e);
        }
    }

    /**
     * Update realm theme to use rms-auth-theme-plugin.
     *
     * @param realmResource the realm resource
     */
    private void updateRealmTheme(RealmResource realmResource) {
        try {
            RealmRepresentation realm = realmResource.toRepresentation();
            realm.setLoginTheme("rms-auth-theme-plugin");
            realm.setAccountTheme("rms-auth-theme-plugin");
            realm.setEmailTheme("rms-auth-theme-plugin");
            realmResource.update(realm);
            log.info("Updated realm theme to rms-auth-theme-plugin for realm: {}", realm.getRealm());
        } catch (Exception e) {
            log.error("Failed to update realm theme for realm: {}", realmResource.toRepresentation().getRealm(), e);
            throw new RuntimeException("Failed to update realm theme", e);
        }
    }

    private RealmRepresentation createRealmRepresentation(String realmName, String tenantName) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setDisplayName(tenantName + " Restaurant");
        realm.setEnabled(true);

        // Registration settings
        realm.setRegistrationAllowed(restaurantProperties.getRegistration().isEnabled());
        realm.setLoginWithEmailAllowed(restaurantProperties.getRegistration().isEmailAsUsername());
        realm.setDuplicateEmailsAllowed(false);
        realm.setResetPasswordAllowed(restaurantProperties.getAuthentication().isResetPasswordAllowed());
        realm.setEditUsernameAllowed(false);
        realm.setVerifyEmail(restaurantProperties.getRegistration().isVerifyEmail());
        realm.setRememberMe(restaurantProperties.getAuthentication().isRememberMe());

        // Theme configuration
        realm.setLoginTheme(restaurantProperties.getTheme().getLoginTheme());
        realm.setAccountTheme(restaurantProperties.getTheme().getAccountTheme());
        realm.setAdminTheme(restaurantProperties.getTheme().getAdminTheme());
        realm.setEmailTheme(restaurantProperties.getTheme().getEmailTheme());

        // Security settings
        realm.setAccessTokenLifespan(restaurantProperties.getSecurity().getAccessTokenLifespan());
        realm.setRefreshTokenMaxReuse(restaurantProperties.getSecurity().getRefreshTokenMaxReuse());
        realm.setSsoSessionIdleTimeout(restaurantProperties.getSecurity().getSsoSessionIdleTimeout());
        realm.setSsoSessionMaxLifespan(restaurantProperties.getSecurity().getSsoSessionMaxLifespan());

        // Password policy
        realm.setPasswordPolicy(buildPasswordPolicy());

        // Internationalization
        realm.setInternationalizationEnabled(true);
        realm.setSupportedLocales(new java.util.HashSet<>(Arrays.asList("en", "es", "fr")));
        realm.setDefaultLocale("en");

        return realm;
    }

    private String buildPasswordPolicy() {
        var policy = restaurantProperties.getSecurity().getPasswordPolicy();
        return String.format(
            "length(%d) and digits(%d) and lowerCase(%d) and upperCase(%d) and specialChars(%d) and notUsername and passwordHistory(%d)",
            policy.getMinLength(),
            policy.getMinDigits(),
            policy.getMinLowerCase(),
            policy.getMinUpperCase(),
            policy.getMinSpecialChars(),
            policy.getPasswordHistory()
        );
    }

    private void createClientScopes(RealmResource realmResource) {
        // Create profile scope
        ClientScopeRepresentation profileScope = new ClientScopeRepresentation();
        profileScope.setName("profile");
        profileScope.setDescription("OpenID Connect built-in scope: profile");
        profileScope.setProtocol("openid-connect");
        realmResource.clientScopes().create(profileScope);

        // Create email scope
        ClientScopeRepresentation emailScope = new ClientScopeRepresentation();
        emailScope.setName("email");
        emailScope.setDescription("OpenID Connect built-in scope: email");
        emailScope.setProtocol("openid-connect");
        realmResource.clientScopes().create(emailScope);

        log.info("Created client scopes for realm: {}", realmResource.toRepresentation().getRealm());
    }

    private void createTenantClient(RealmResource realmResource, String tenantId, String clientType) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(tenantId + "_" + clientType);
        client.setName(tenantId + " " + clientType.substring(0, 1).toUpperCase() + clientType.substring(1) + " Client");
        client.setDescription(clientType.substring(0, 1).toUpperCase() + clientType.substring(1) + " client for " + tenantId + " tenant");
        client.setEnabled(true);
        client.setPublicClient("mobile".equals(clientType));
        client.setStandardFlowEnabled(true);
        client.setImplicitFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(false);
        client.setAuthorizationServicesEnabled(false);

        // Set redirect URIs based on client type
        if ("web".equals(clientType)) {
            client.setRedirectUris(
                Arrays.asList(
                    "http://localhost:8082/login/oauth2/code/oidc",
                    "https://" + tenantId + ".yourdomain.com/login/oauth2/code/oidc"
                )
            );
            client.setWebOrigins(Arrays.asList("http://localhost:8082", "https://" + tenantId + ".yourdomain.com"));
        } else if ("mobile".equals(clientType)) {
            client.setRedirectUris(Arrays.asList("com." + tenantId + ".app://oauth/callback", "http://localhost:3000/callback"));
        }

        // Set client secret for web clients only
        if ("web".equals(clientType)) {
            client.setSecret(generateClientSecret());
        }

        // Set default client scopes
        client.setDefaultClientScopes(Arrays.asList("openid", "profile", "email", "offline_access"));

        realmResource.clients().create(client);
        log.info("Created {} client: {} for realm: {}", clientType, client.getClientId(), realmResource.toRepresentation().getRealm());
    }

    private void createRealmRoles(RealmResource realmResource) {
        // Restaurant-specific roles
        createRole(realmResource, "ROLE_ADMIN", "System Administrator - Full access to all restaurant operations");
        createRole(realmResource, "ROLE_MANAGER", "Restaurant Manager - Manages restaurant operations and staff");
        createRole(realmResource, "ROLE_SUPERVISOR", "Supervisor - Oversees daily operations and staff");
        createRole(realmResource, "ROLE_WAITER", "Waiter/Server - Takes orders and serves customers");
        createRole(realmResource, "ROLE_CHEF", "Chef - Prepares food and manages kitchen operations");
        createRole(realmResource, "ROLE_CASHIER", "Cashier - Handles payments and order processing");
        createRole(realmResource, "ROLE_CUSTOMER", "Customer - Places orders and makes reservations");
        createRole(realmResource, "ROLE_ANONYMOUS", "Anonymous - Limited access for non-authenticated users");

        log.info("Created restaurant roles for realm: {}", realmResource.toRepresentation().getRealm());
    }

    private void createRole(RealmResource realmResource, String roleName, String description) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setDescription(description);
        realmResource.roles().create(role);
    }

    private String generateClientSecret() {
        // Generate a secure random client secret
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public void deleteTenantRealm(String tenantId) {
        try {
            String realmName = tenantId + "_realm";
            keycloakAdmin.realm(realmName).remove();
            log.info("Deleted realm: {}", realmName);
        } catch (Exception e) {
            log.error("Failed to delete tenant realm for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to delete Keycloak realm for tenant: " + tenantId, e);
        }
    }
}

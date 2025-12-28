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
    private final String rmsServiceClientSecret;

    public KeycloakRealmService(
        Keycloak keycloakAdmin,
        RestaurantKeycloakProperties restaurantProperties,
        @org.springframework.beans.factory.annotation.Value("${rms-service.client-secret:}") String rmsServiceClientSecret
    ) {
        this.keycloakAdmin = keycloakAdmin;
        this.restaurantProperties = restaurantProperties;
        this.rmsServiceClientSecret = rmsServiceClientSecret;
    }

    public void createTenantRealm(String tenantId, String tenantName) {
        String realmName = tenantId + "_realm";
        boolean realmCreated = false;
        boolean clientScopesCreated = false;
        boolean webClientCreated = false;
        boolean mobileClientCreated = false;
        boolean rmsServiceClientCreated = false;
        boolean rolesCreated = false;
        boolean themeUpdated = false;
        boolean flowsCreated = false;

        try {
            // Step 1: Create realm
            try {
                RealmRepresentation realm = createRealmRepresentation(realmName, tenantName);
                keycloakAdmin.realms().create(realm);
                realmCreated = true;
                log.info("Step 1: Created realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to create realm: {}", realmName, e);
                throw new RuntimeException("Failed to create realm: " + realmName, e);
            }

            // Get realm resource for further configuration
            RealmResource realmResource = keycloakAdmin.realm(realmName);

            // Step 2: Create client scopes
            try {
                createClientScopes(realmResource);
                clientScopesCreated = true;
                log.info("Step 2: Created client scopes for realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to create client scopes for realm: {}", realmName, e);
                rollbackRealmCreation(
                    tenantId,
                    realmName,
                    realmCreated,
                    clientScopesCreated,
                    webClientCreated,
                    mobileClientCreated,
                    false,
                    rolesCreated,
                    themeUpdated,
                    flowsCreated
                );
                throw new RuntimeException("Failed to create client scopes", e);
            }

            // Step 3: Create web client
            try {
                createTenantClient(realmResource, tenantId, "web");
                webClientCreated = true;
                log.info("Step 3: Created web client for realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to create web client for realm: {}", realmName, e);
                rollbackRealmCreation(
                    tenantId,
                    realmName,
                    realmCreated,
                    clientScopesCreated,
                    webClientCreated,
                    mobileClientCreated,
                    false,
                    rolesCreated,
                    themeUpdated,
                    flowsCreated
                );
                throw new RuntimeException("Failed to create web client", e);
            }

            // Step 4: Create mobile client
            try {
                createTenantClient(realmResource, tenantId, "mobile");
                mobileClientCreated = true;
                log.info("Step 4: Created mobile client for realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to create mobile client for realm: {}", realmName, e);
                rollbackRealmCreation(
                    tenantId,
                    realmName,
                    realmCreated,
                    clientScopesCreated,
                    webClientCreated,
                    mobileClientCreated,
                    rmsServiceClientCreated,
                    rolesCreated,
                    themeUpdated,
                    flowsCreated
                );
                throw new RuntimeException("Failed to create mobile client", e);
            }

            // Step 5: Create rms-service client
            try {
                createRmsServiceClient(realmResource);
                rmsServiceClientCreated = true;
                log.info("Step 5: Created rms-service client for realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to create rms-service client for realm: {}", realmName, e);
                rollbackRealmCreation(
                    tenantId,
                    realmName,
                    realmCreated,
                    clientScopesCreated,
                    webClientCreated,
                    mobileClientCreated,
                    rmsServiceClientCreated,
                    rolesCreated,
                    themeUpdated,
                    flowsCreated
                );
                throw new RuntimeException("Failed to create rms-service client", e);
            }

            // Step 6: Create realm roles
            try {
                createRealmRoles(realmResource);
                rolesCreated = true;
                log.info("Step 6: Created realm roles for realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to create realm roles for realm: {}", realmName, e);
                rollbackRealmCreation(
                    tenantId,
                    realmName,
                    realmCreated,
                    clientScopesCreated,
                    webClientCreated,
                    mobileClientCreated,
                    rmsServiceClientCreated,
                    rolesCreated,
                    themeUpdated,
                    flowsCreated
                );
                throw new RuntimeException("Failed to create realm roles", e);
            }

            // Step 7: Update realm theme
            try {
                updateRealmTheme(realmResource);
                themeUpdated = true;
                log.info("Step 7: Updated realm theme for realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to update realm theme for realm: {}", realmName, e);
                rollbackRealmCreation(
                    tenantId,
                    realmName,
                    realmCreated,
                    clientScopesCreated,
                    webClientCreated,
                    mobileClientCreated,
                    rmsServiceClientCreated,
                    rolesCreated,
                    themeUpdated,
                    flowsCreated
                );
                throw new RuntimeException("Failed to update realm theme", e);
            }

            // Step 8: Copy browser flow and modify it with phone auto-reg form
            try {
                KeycloakFlowService flowService = new KeycloakFlowService(keycloakAdmin);
                flowService.copyAndModifyBrowserFlow(realmName);
                flowsCreated = true;
                log.info("Step 8: Created browser flow for realm: {}", realmName);
            } catch (Exception e) {
                log.error("Failed to create browser flow for realm: {}", realmName, e);
                rollbackRealmCreation(
                    tenantId,
                    realmName,
                    realmCreated,
                    clientScopesCreated,
                    webClientCreated,
                    mobileClientCreated,
                    rmsServiceClientCreated,
                    rolesCreated,
                    themeUpdated,
                    flowsCreated
                );
                throw new RuntimeException("Failed to create browser flow", e);
            }

            log.info("Successfully configured tenant realm: {}", realmName);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions (they already have rollback logic)
            throw e;
        } catch (Exception e) {
            log.error("Failed to create tenant realm for tenant: {}", tenantId, e);
            // Final rollback attempt
            rollbackRealmCreation(
                tenantId,
                realmName,
                realmCreated,
                clientScopesCreated,
                webClientCreated,
                mobileClientCreated,
                false,
                rolesCreated,
                themeUpdated,
                flowsCreated
            );
            throw new RuntimeException("Failed to create Keycloak realm for tenant: " + tenantId, e);
        }
    }

    /**
     * Rollback realm creation by deleting the realm if it was created.
     * Deleting the realm will automatically delete all associated resources (clients, roles, flows, etc.).
     *
     * @param tenantId the tenant ID
     * @param realmName the realm name
     * @param realmCreated whether the realm was created
     * @param clientScopesCreated whether client scopes were created
     * @param webClientCreated whether web client was created
     * @param mobileClientCreated whether mobile client was created
     * @param rmsServiceClientCreated whether rms-service client was created
     * @param rolesCreated whether roles were created
     * @param themeUpdated whether theme was updated
     * @param flowsCreated whether flows were created
     */
    private void rollbackRealmCreation(
        String tenantId,
        String realmName,
        boolean realmCreated,
        boolean clientScopesCreated,
        boolean webClientCreated,
        boolean mobileClientCreated,
        boolean rmsServiceClientCreated,
        boolean rolesCreated,
        boolean themeUpdated,
        boolean flowsCreated
    ) {
        log.warn("Rolling back realm creation for realm: {}", realmName);

        // If realm was created, delete it (this will automatically delete all associated resources)
        if (realmCreated) {
            try {
                keycloakAdmin.realm(realmName).remove();
                log.info("Rollback: Deleted realm: {} (and all associated resources)", realmName);
            } catch (Exception e) {
                log.error("Rollback: Failed to delete realm: {}", realmName, e);
                // Note: We log the error but don't throw, as this is cleanup during rollback
            }
        } else {
            log.debug("Rollback: Realm {} was not created, nothing to rollback", realmName);
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

    /**
     * Create rms-service client in the tenant realm.
     * This client is used by the RMS Service to validate tokens from this tenant realm.
     *
     * @param realmResource the realm resource
     */
    private void createRmsServiceClient(RealmResource realmResource) {
        if (rmsServiceClientSecret == null || rmsServiceClientSecret.isEmpty()) {
            log.warn("rms-service.client-secret is not configured. Skipping rms-service client creation.");
            return;
        }

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("rms-service");
        client.setName("RMS Service Client");
        client.setDescription("Service client for RMS Service to validate tokens from this tenant realm");
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setStandardFlowEnabled(false);
        client.setImplicitFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(false);
        client.setServiceAccountsEnabled(true);
        client.setAuthorizationServicesEnabled(false);

        // Set client secret
        client.setSecret(rmsServiceClientSecret);

        // Set redirect URIs (not needed for service account, but set for completeness)
        client.setRedirectUris(Arrays.asList("*"));
        client.setWebOrigins(Arrays.asList("*"));

        // Set default client scopes
        client.setDefaultClientScopes(Arrays.asList("openid", "profile", "email", "offline_access"));

        realmResource.clients().create(client);
        log.info("Created rms-service client for realm: {}", realmResource.toRepresentation().getRealm());
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

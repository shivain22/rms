package com.atparui.rms.service;

import com.atparui.rms.config.RestaurantKeycloakProperties;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RestaurantUserService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantUserService.class);

    private final Keycloak keycloakAdmin;
    private final RestaurantKeycloakProperties restaurantProperties;

    // Restaurant role hierarchy
    private static final Map<String, Integer> ROLE_HIERARCHY = Map.of(
        "ROLE_CUSTOMER",
        1,
        "ROLE_WAITER",
        2,
        "ROLE_CHEF",
        3,
        "ROLE_CASHIER",
        4,
        "ROLE_SUPERVISOR",
        5,
        "ROLE_MANAGER",
        6,
        "ROLE_ADMIN",
        7
    );

    public RestaurantUserService(Keycloak keycloakAdmin, RestaurantKeycloakProperties restaurantProperties) {
        this.keycloakAdmin = keycloakAdmin;
        this.restaurantProperties = restaurantProperties;
    }

    public String createRestaurantUser(String realmName, String email, String firstName, String lastName, String role, String branchId) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);

            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(!restaurantProperties.getRegistration().isVerifyEmail());

            // Add custom attributes for restaurant context
            user.singleAttribute("branchId", branchId);
            user.singleAttribute("role", role);
            user.singleAttribute("userType", "restaurant-staff");

            Response response = realmResource.users().create(user);

            if (response.getStatus() == 201) {
                String userId = extractUserIdFromLocation(response.getLocation().toString());

                // Assign role to user
                assignRoleToUser(realmResource, userId, role);

                // Set temporary password if needed
                setTemporaryPassword(realmResource, userId, generateTemporaryPassword());

                log.info("Created restaurant user: {} with role: {} in realm: {}", email, role, realmName);
                return userId;
            } else {
                throw new RuntimeException("Failed to create user, status: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Failed to create restaurant user: {} in realm: {}", email, realmName, e);
            throw new RuntimeException("Failed to create restaurant user", e);
        }
    }

    public void assignRoleToUser(RealmResource realmResource, String userId, String roleName) {
        try {
            UserResource userResource = realmResource.users().get(userId);
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Arrays.asList(role));

            log.info("Assigned role: {} to user: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Failed to assign role: {} to user: {}", roleName, userId, e);
            throw new RuntimeException("Failed to assign role to user", e);
        }
    }

    public void updateUserRole(String realmName, String userId, String newRole) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);
            UserResource userResource = realmResource.users().get(userId);

            // Remove all existing restaurant roles
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
            List<RoleRepresentation> rolesToRemove = currentRoles
                .stream()
                .filter(role -> ROLE_HIERARCHY.containsKey(role.getName()))
                .toList();

            if (!rolesToRemove.isEmpty()) {
                userResource.roles().realmLevel().remove(rolesToRemove);
            }

            // Assign new role
            assignRoleToUser(realmResource, userId, newRole);

            // Update user attributes
            UserRepresentation user = userResource.toRepresentation();
            user.singleAttribute("role", newRole);
            userResource.update(user);

            log.info("Updated user: {} role to: {} in realm: {}", userId, newRole, realmName);
        } catch (Exception e) {
            log.error("Failed to update user role for user: {} in realm: {}", userId, realmName, e);
            throw new RuntimeException("Failed to update user role", e);
        }
    }

    public void assignUserToBranch(String realmName, String userId, String branchId) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);
            UserResource userResource = realmResource.users().get(userId);

            UserRepresentation user = userResource.toRepresentation();
            user.singleAttribute("branchId", branchId);
            userResource.update(user);

            log.info("Assigned user: {} to branch: {} in realm: {}", userId, branchId, realmName);
        } catch (Exception e) {
            log.error("Failed to assign user: {} to branch: {} in realm: {}", userId, branchId, realmName, e);
            throw new RuntimeException("Failed to assign user to branch", e);
        }
    }

    public List<UserRepresentation> getUsersByRole(String realmName, String role) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);
            RoleRepresentation roleRep = realmResource.roles().get(role).toRepresentation();
            return realmResource.roles().get(role).getUserMembers();
        } catch (Exception e) {
            log.error("Failed to get users by role: {} in realm: {}", role, realmName, e);
            throw new RuntimeException("Failed to get users by role", e);
        }
    }

    public List<UserRepresentation> getUsersByBranch(String realmName, String branchId) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);
            return realmResource
                .users()
                .search(null, null, null, null, null, null)
                .stream()
                .filter(user -> branchId.equals(user.getAttributes().get("branchId")))
                .toList();
        } catch (Exception e) {
            log.error("Failed to get users by branch: {} in realm: {}", branchId, realmName, e);
            throw new RuntimeException("Failed to get users by branch", e);
        }
    }

    public boolean hasPermission(String userRole, String requiredRole) {
        Integer userLevel = ROLE_HIERARCHY.get(userRole);
        Integer requiredLevel = ROLE_HIERARCHY.get(requiredRole);

        return userLevel != null && requiredLevel != null && userLevel >= requiredLevel;
    }

    private void setTemporaryPassword(RealmResource realmResource, String userId, String password) {
        try {
            UserResource userResource = realmResource.users().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(true);

            userResource.resetPassword(credential);
        } catch (Exception e) {
            log.error("Failed to set temporary password for user: {}", userId, e);
        }
    }

    private String generateTemporaryPassword() {
        // Generate a secure temporary password
        return "TempPass" + (System.currentTimeMillis() % 10000);
    }

    private String extractUserIdFromLocation(String location) {
        return location.substring(location.lastIndexOf('/') + 1);
    }
}

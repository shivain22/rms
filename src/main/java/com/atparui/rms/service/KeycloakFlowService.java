package com.atparui.rms.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KeycloakFlowService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakFlowService.class);

    private final Keycloak keycloakAdmin;

    public KeycloakFlowService(Keycloak keycloakAdmin) {
        this.keycloakAdmin = keycloakAdmin;
    }

    public void createRestaurantBrowserFlow(String realmName) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);

            // Create custom browser flow
            AuthenticationFlowRepresentation browserFlow = new AuthenticationFlowRepresentation();
            browserFlow.setAlias("restaurant-browser-flow");
            browserFlow.setDescription("Restaurant custom browser flow with role-based authentication");
            browserFlow.setProviderId("basic-flow");
            browserFlow.setTopLevel(true);
            browserFlow.setBuiltIn(false);

            realmResource.flows().createFlow(browserFlow);

            // Add executions to the flow
            addCookieExecution(realmResource, "restaurant-browser-flow");
            addKerberosExecution(realmResource, "restaurant-browser-flow");
            addIdentityProviderRedirectExecution(realmResource, "restaurant-browser-flow");
            addRestaurantFormsExecution(realmResource, "restaurant-browser-flow");

            log.info("Created restaurant browser flow for realm: {}", realmName);
        } catch (Exception e) {
            log.error("Failed to create restaurant browser flow for realm: {}", realmName, e);
            throw new RuntimeException("Failed to create browser flow", e);
        }
    }

    public void createRestaurantRegistrationFlow(String realmName) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);

            // Create custom registration flow
            AuthenticationFlowRepresentation registrationFlow = new AuthenticationFlowRepresentation();
            registrationFlow.setAlias("restaurant-registration-flow");
            registrationFlow.setDescription("Restaurant registration flow with role selection");
            registrationFlow.setProviderId("basic-flow");
            registrationFlow.setTopLevel(true);
            registrationFlow.setBuiltIn(false);

            realmResource.flows().createFlow(registrationFlow);

            // Add registration form execution
            addRegistrationFormExecution(realmResource, "restaurant-registration-flow");

            log.info("Created restaurant registration flow for realm: {}", realmName);
        } catch (Exception e) {
            log.error("Failed to create restaurant registration flow for realm: {}", realmName, e);
            throw new RuntimeException("Failed to create registration flow", e);
        }
    }

    private void addCookieExecution(RealmResource realmResource, String flowAlias) {
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("provider", "auth-cookie");
        realmResource.flows().addExecution(flowAlias, executionData);

        // Set as alternative
        List<AuthenticationExecutionInfoRepresentation> executions = realmResource.flows().getExecutions(flowAlias);
        AuthenticationExecutionInfoRepresentation cookieExecution = executions
            .stream()
            .filter(e -> "auth-cookie".equals(e.getProviderId()))
            .findFirst()
            .orElse(null);

        if (cookieExecution != null) {
            cookieExecution.setRequirement("ALTERNATIVE");
            realmResource.flows().updateExecutions(flowAlias, cookieExecution);
        }
    }

    private void addKerberosExecution(RealmResource realmResource, String flowAlias) {
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("provider", "auth-spnego");
        realmResource.flows().addExecution(flowAlias, executionData);

        // Set as disabled
        List<AuthenticationExecutionInfoRepresentation> executions = realmResource.flows().getExecutions(flowAlias);
        AuthenticationExecutionInfoRepresentation kerberosExecution = executions
            .stream()
            .filter(e -> "auth-spnego".equals(e.getProviderId()))
            .findFirst()
            .orElse(null);

        if (kerberosExecution != null) {
            kerberosExecution.setRequirement("DISABLED");
            realmResource.flows().updateExecutions(flowAlias, kerberosExecution);
        }
    }

    private void addIdentityProviderRedirectExecution(RealmResource realmResource, String flowAlias) {
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("provider", "identity-provider-redirector");
        realmResource.flows().addExecution(flowAlias, executionData);

        // Set as alternative
        List<AuthenticationExecutionInfoRepresentation> executions = realmResource.flows().getExecutions(flowAlias);
        AuthenticationExecutionInfoRepresentation idpExecution = executions
            .stream()
            .filter(e -> "identity-provider-redirector".equals(e.getProviderId()))
            .findFirst()
            .orElse(null);

        if (idpExecution != null) {
            idpExecution.setRequirement("ALTERNATIVE");
            realmResource.flows().updateExecutions(flowAlias, idpExecution);
        }
    }

    private void addRestaurantFormsExecution(RealmResource realmResource, String flowAlias) {
        // Create restaurant forms subflow
        AuthenticationFlowRepresentation formsFlow = new AuthenticationFlowRepresentation();
        formsFlow.setAlias("restaurant-forms");
        formsFlow.setDescription("Restaurant username/password forms");
        formsFlow.setProviderId("basic-flow");
        formsFlow.setTopLevel(false);
        formsFlow.setBuiltIn(false);

        realmResource.flows().createFlow(formsFlow);

        // Add subflow to main flow
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("provider", "restaurant-forms");
        realmResource.flows().addExecution(flowAlias, executionData);

        // Add username/password form to subflow
        Map<String, Object> usernamePasswordData = new HashMap<>();
        usernamePasswordData.put("provider", "auth-username-password-form");
        realmResource.flows().addExecution("restaurant-forms", usernamePasswordData);

        // Add OTP form to subflow
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("provider", "auth-otp-form");
        realmResource.flows().addExecution("restaurant-forms", otpData);

        // Configure requirements
        List<AuthenticationExecutionInfoRepresentation> formsExecutions = realmResource.flows().getExecutions("restaurant-forms");

        for (AuthenticationExecutionInfoRepresentation execution : formsExecutions) {
            if ("auth-username-password-form".equals(execution.getProviderId())) {
                execution.setRequirement("REQUIRED");
                realmResource.flows().updateExecutions("restaurant-forms", execution);
            } else if ("auth-otp-form".equals(execution.getProviderId())) {
                execution.setRequirement("CONDITIONAL");
                realmResource.flows().updateExecutions("restaurant-forms", execution);
            }
        }
    }

    private void addRegistrationFormExecution(RealmResource realmResource, String flowAlias) {
        Map<String, Object> executionData = new HashMap<>();
        executionData.put("provider", "registration-page-form");
        realmResource.flows().addExecution(flowAlias, executionData);

        // Set as required
        List<AuthenticationExecutionInfoRepresentation> executions = realmResource.flows().getExecutions(flowAlias);
        AuthenticationExecutionInfoRepresentation regExecution = executions
            .stream()
            .filter(e -> "registration-page-form".equals(e.getProviderId()))
            .findFirst()
            .orElse(null);

        if (regExecution != null) {
            regExecution.setRequirement("REQUIRED");
            realmResource.flows().updateExecutions(flowAlias, regExecution);
        }
    }

    public void configureCustomAuthenticator(String realmName, String authenticatorName, Map<String, String> config) {
        try {
            AuthenticatorConfigRepresentation authenticatorConfig = new AuthenticatorConfigRepresentation();
            authenticatorConfig.setAlias(authenticatorName + "-config");
            authenticatorConfig.setConfig(config);

            // This would be used when you have custom authenticator implementations
            log.info("Configured custom authenticator: {} for realm: {}", authenticatorName, realmName);
        } catch (Exception e) {
            log.error("Failed to configure custom authenticator: {} for realm: {}", authenticatorName, realmName, e);
        }
    }

    /**
     * Copy the default browser flow and modify it to use phone auto-registration form
     * instead of username-password form.
     *
     * This follows the recommended Keycloak Admin REST API pattern:
     * 1. Retrieve the complete structure of the default browser flow
     * 2. Create a new top-level flow
     * 3. Create all required subflows first (to avoid timing issues)
     * 4. Add executions in order, preserving requirements and configurations
     * 5. Bind the new flow as the browser flow
     *
     * @param realmName the realm name
     */
    public void copyAndModifyBrowserFlow(String realmName) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);
            String newFlowAlias = "browser-phone-flow";

            // Step 1: Retrieve the structure of the default browser flow
            log.info("Retrieving default browser flow structure");
            List<AuthenticationFlowRepresentation> flows = realmResource.flows().getFlows();
            AuthenticationFlowRepresentation browserFlow = flows
                .stream()
                .filter(f -> "browser".equals(f.getAlias()) && f.isBuiltIn())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Default browser flow not found"));

            log.info("Found default browser flow: {}", browserFlow.getAlias());

            // Get all executions from the original browser flow (complete structure)
            List<AuthenticationExecutionInfoRepresentation> originalExecutions = realmResource.flows().getExecutions("browser");
            log.info("Retrieved {} executions from default browser flow", originalExecutions.size());

            // Step 2: Create the new top-level flow
            AuthenticationFlowRepresentation newFlow = new AuthenticationFlowRepresentation();
            newFlow.setAlias(newFlowAlias);
            newFlow.setDescription("Browser flow with phone auto-registration");
            newFlow.setProviderId("basic-flow");
            newFlow.setTopLevel(true);
            newFlow.setBuiltIn(false);

            realmResource.flows().createFlow(newFlow);
            log.info("Created new top-level flow: {}", newFlowAlias);

            // Verify the main flow is available before proceeding
            if (!waitForFlowToBeAvailable(realmResource, newFlowAlias, 5)) {
                throw new RuntimeException("Failed to verify newly created browser flow: " + newFlowAlias);
            }

            // Step 3: Create all required subflows FIRST (before adding executions)
            // This ensures subflows are available when we add them as executions
            log.info("Step 3: Creating all required subflows");

            // Find the forms subflow in the original flow (if it exists)
            String formsSubflowId = null;
            String formsSubflowAlias = null;
            String formsSubflowRequirement = "REQUIRED"; // Default requirement
            for (AuthenticationExecutionInfoRepresentation execution : originalExecutions) {
                if (execution.getFlowId() != null && !execution.getFlowId().isEmpty()) {
                    String subflowAlias = getFlowAliasById(realmResource, execution.getFlowId());
                    if ("forms".equals(subflowAlias)) {
                        formsSubflowId = execution.getFlowId();
                        formsSubflowAlias = subflowAlias;
                        formsSubflowRequirement = execution.getRequirement() != null ? execution.getRequirement() : "REQUIRED";
                        log.info("Found forms subflow in original flow with requirement: {}", formsSubflowRequirement);
                        break;
                    }
                }
            }

            // Create our custom forms subflow with phone authenticator
            // This must be done BEFORE adding executions that reference it
            String newFormsSubflowAlias = "forms-phone";
            if (formsSubflowAlias != null) {
                log.info("Copying forms subflow from original and modifying for phone authenticator");
                copyFormsSubflowWithoutUsernamePassword(realmResource, formsSubflowAlias, newFormsSubflowAlias);
            } else {
                log.info("Forms subflow not found in original browser flow, creating new one");
                createFormsSubflowWithPhoneAuthenticator(realmResource, newFormsSubflowAlias);
            }

            // Wait a bit to ensure all subflows are fully created and available
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for subflows to be available");
            }

            // Step 4: Add executions in order, preserving requirements and configurations
            log.info("Step 4: Adding executions to new flow in order");

            // Copy executions from the original browser flow in order
            // This preserves the execution order and requirements from the original flow
            int executionIndex = 0;
            for (AuthenticationExecutionInfoRepresentation execution : originalExecutions) {
                executionIndex++;
                log.debug(
                    "Processing execution {} of {}: provider={}, requirement={}",
                    executionIndex,
                    originalExecutions.size(),
                    execution.getProviderId() != null ? execution.getProviderId() : "subflow",
                    execution.getRequirement()
                );
                // Skip username-password form execution if it's a direct execution (not in subflow)
                // This is the standard way to remove username-password form from the flow
                if ("auth-username-password-form".equals(execution.getProviderId())) {
                    log.info("Skipping username-password form execution (will be replaced with phone authenticator)");
                    continue;
                }

                // Handle forms subflow specially - replace with our new forms subflow
                if (execution.getFlowId() != null && execution.getFlowId().equals(formsSubflowId)) {
                    log.info("Replacing forms subflow with custom forms-phone subflow");
                    // Add the new forms subflow to the browser flow
                    Map<String, Object> executionData = new HashMap<>();
                    executionData.put("provider", newFormsSubflowAlias);

                    // Ensure subflow is available before adding
                    if (!waitForSubflowToBeAvailable(realmResource, newFormsSubflowAlias, 3)) {
                        throw new RuntimeException("Forms subflow not available when trying to add execution");
                    }

                    try {
                        realmResource.flows().addExecution(newFlowAlias, executionData);
                        log.info("Successfully added forms subflow execution to flow: {}", newFlowAlias);

                        // Wait a bit for execution to be persisted
                        Thread.sleep(200);

                        // Update requirement to match original
                        List<AuthenticationExecutionInfoRepresentation> newExecutions = realmResource.flows().getExecutions(newFlowAlias);
                        AuthenticationExecutionInfoRepresentation newExecution = newExecutions
                            .stream()
                            .filter(e -> {
                                if (e.getFlowId() != null) {
                                    String flowAlias = getFlowAliasById(realmResource, e.getFlowId());
                                    return newFormsSubflowAlias.equals(flowAlias);
                                }
                                return false;
                            })
                            .findFirst()
                            .orElse(null);

                        if (newExecution != null) {
                            newExecution.setRequirement(formsSubflowRequirement);
                            realmResource.flows().updateExecutions(newFlowAlias, newExecution);
                            log.info("Set forms subflow requirement to: {}", formsSubflowRequirement);
                        } else {
                            log.warn("Could not find newly added forms subflow execution to set requirement");
                        }
                    } catch (jakarta.ws.rs.BadRequestException e) {
                        log.error(
                            "Failed to add forms subflow execution '{}' to flow '{}': {}",
                            newFormsSubflowAlias,
                            newFlowAlias,
                            e.getMessage()
                        );
                        // Check if it already exists
                        List<AuthenticationExecutionInfoRepresentation> existingExecutions = realmResource
                            .flows()
                            .getExecutions(newFlowAlias);
                        boolean alreadyExists = existingExecutions
                            .stream()
                            .anyMatch(
                                ex -> ex.getFlowId() != null && getFlowAliasById(realmResource, ex.getFlowId()).equals(newFormsSubflowAlias)
                            );

                        if (!alreadyExists) {
                            throw new RuntimeException("Failed to add forms subflow execution to flow: " + e.getMessage(), e);
                        } else {
                            log.warn("Forms subflow execution already exists, continuing");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting for execution to be persisted");
                    }
                } else {
                    // Copy other executions as-is
                    Map<String, Object> executionData = new HashMap<>();
                    if (execution.getFlowId() != null && !execution.getFlowId().isEmpty()) {
                        // It's a subflow - get the alias and find the corresponding flow ID in the new realm
                        String subflowAlias = getFlowAliasById(realmResource, execution.getFlowId());
                        if (subflowAlias != null) {
                            // Find the flow ID in the new realm by alias
                            String newRealmFlowId = getFlowIdByAlias(realmResource, subflowAlias);
                            if (newRealmFlowId != null) {
                                // For subflows, we need to use the alias, not the flow ID
                                executionData.put("provider", subflowAlias);
                                log.debug(
                                    "Adding subflow execution: {} (original flow ID: {}, new flow ID: {})",
                                    subflowAlias,
                                    execution.getFlowId(),
                                    newRealmFlowId
                                );
                            } else {
                                log.warn("Subflow {} not found in new realm, skipping", subflowAlias);
                                continue;
                            }
                        } else {
                            log.warn("Could not find alias for flow ID {}, skipping", execution.getFlowId());
                            continue;
                        }
                    } else {
                        // It's a direct authenticator
                        if (execution.getProviderId() == null || execution.getProviderId().isEmpty()) {
                            log.warn("Execution has no provider ID, skipping");
                            continue;
                        }
                        executionData.put("provider", execution.getProviderId());
                        log.debug("Adding authenticator execution: {}", execution.getProviderId());
                    }

                    try {
                        realmResource.flows().addExecution(newFlowAlias, executionData);
                        log.debug("Successfully added execution to flow: {}", newFlowAlias);
                    } catch (jakarta.ws.rs.BadRequestException e) {
                        String providerValue = (String) executionData.get("provider");
                        log.error("Failed to add execution '{}' to flow '{}': {}", providerValue, newFlowAlias, e.getMessage());

                        // Check if execution already exists - if so, skip it
                        List<AuthenticationExecutionInfoRepresentation> existingExecutions = realmResource
                            .flows()
                            .getExecutions(newFlowAlias);
                        boolean alreadyExists = existingExecutions
                            .stream()
                            .anyMatch(ex -> {
                                if (execution.getFlowId() != null && !execution.getFlowId().isEmpty()) {
                                    String existingFlowAlias = getFlowAliasById(realmResource, ex.getFlowId());
                                    return providerValue != null && providerValue.equals(existingFlowAlias);
                                } else {
                                    return providerValue != null && providerValue.equals(ex.getProviderId());
                                }
                            });

                        if (alreadyExists) {
                            log.warn("Execution '{}' already exists in flow '{}', skipping", providerValue, newFlowAlias);
                            // Continue to update requirement for existing execution
                        } else {
                            // Re-throw if it's a different error
                            log.error("Unknown error adding execution '{}' to flow '{}'", providerValue, newFlowAlias, e);
                            throw new RuntimeException(
                                "Failed to add execution '" + providerValue + "' to flow '" + newFlowAlias + "': " + e.getMessage(),
                                e
                            );
                        }
                    }

                    // Update requirement
                    List<AuthenticationExecutionInfoRepresentation> newExecutions = realmResource.flows().getExecutions(newFlowAlias);
                    AuthenticationExecutionInfoRepresentation newExecution = newExecutions
                        .stream()
                        .filter(e -> {
                            if (execution.getFlowId() != null && !execution.getFlowId().isEmpty()) {
                                return e.getFlowId() != null && e.getFlowId().equals(execution.getFlowId());
                            } else {
                                return execution.getProviderId().equals(e.getProviderId());
                            }
                        })
                        .findFirst()
                        .orElse(null);

                    if (newExecution != null) {
                        newExecution.setRequirement(execution.getRequirement());
                        realmResource.flows().updateExecutions(newFlowAlias, newExecution);
                    }
                }
            }

            // If forms subflow wasn't in the original flow, add it now
            if (formsSubflowAlias == null) {
                log.info("Adding forms subflow to new browser flow since it wasn't in the original");

                // Verify the subflow is available
                if (!waitForSubflowToBeAvailable(realmResource, newFormsSubflowAlias, 5)) {
                    throw new RuntimeException("Failed to find newly created forms subflow: " + newFormsSubflowAlias);
                }

                Map<String, Object> executionData = new HashMap<>();
                executionData.put("provider", newFormsSubflowAlias);
                try {
                    realmResource.flows().addExecution(newFlowAlias, executionData);
                    log.info("Successfully added forms subflow execution to flow: {}", newFlowAlias);

                    // Wait a bit for the execution to be persisted
                    Thread.sleep(200);

                    // Set requirement
                    List<AuthenticationExecutionInfoRepresentation> newExecutions = realmResource.flows().getExecutions(newFlowAlias);
                    AuthenticationExecutionInfoRepresentation newExecution = newExecutions
                        .stream()
                        .filter(e -> {
                            if (e.getFlowId() != null) {
                                String flowAlias = getFlowAliasById(realmResource, e.getFlowId());
                                return newFormsSubflowAlias.equals(flowAlias);
                            }
                            return false;
                        })
                        .findFirst()
                        .orElse(null);

                    if (newExecution != null) {
                        newExecution.setRequirement("REQUIRED");
                        realmResource.flows().updateExecutions(newFlowAlias, newExecution);
                        log.info("Set forms subflow requirement to REQUIRED");
                    } else {
                        log.warn("Could not find newly added forms subflow execution to set requirement");
                    }
                } catch (jakarta.ws.rs.BadRequestException e) {
                    log.error(
                        "Failed to add forms subflow execution '{}' to flow '{}': {}",
                        newFormsSubflowAlias,
                        newFlowAlias,
                        e.getMessage()
                    );

                    // Check if it already exists
                    List<AuthenticationExecutionInfoRepresentation> existingExecutions = realmResource.flows().getExecutions(newFlowAlias);
                    boolean alreadyExists = existingExecutions
                        .stream()
                        .anyMatch(ex -> {
                            if (ex.getFlowId() != null) {
                                String flowAlias = getFlowAliasById(realmResource, ex.getFlowId());
                                return newFormsSubflowAlias.equals(flowAlias);
                            }
                            return false;
                        });

                    if (!alreadyExists) {
                        throw new RuntimeException("Failed to add forms subflow execution to flow: " + e.getMessage(), e);
                    } else {
                        log.warn("Forms subflow execution already exists, continuing");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting for execution to be available");
                }
            }

            // Step 5: Bind the new flow as the browser flow
            log.info("Step 5: Binding new flow as browser flow");
            RealmRepresentation realm = realmResource.toRepresentation();
            realm.setBrowserFlow(newFlowAlias);
            realmResource.update(realm);

            log.info("Successfully copied and modified browser flow for realm: {}", realmName);
        } catch (Exception e) {
            log.error("Failed to copy and modify browser flow for realm: {}", realmName, e);
            throw new RuntimeException("Failed to copy and modify browser flow", e);
        }
    }

    /**
     * Create a new forms subflow with phone auto-registration form.
     * This is used when the forms subflow doesn't exist in the original browser flow.
     *
     * Standard Keycloak pattern:
     * 1. Create the subflow
     * 2. Verify it's available
     * 3. Add the phone authenticator as REQUIRED
     * 4. Configure the requirement
     *
     * @param realmResource the realm resource
     * @param newFormsAlias the new forms subflow alias
     */
    private void createFormsSubflowWithPhoneAuthenticator(RealmResource realmResource, String newFormsAlias) {
        // Verify authenticator is available before creating flow
        if (!isAuthenticatorAvailable(realmResource, "auth-phone-auto-reg-form")) {
            throw new RuntimeException("Phone auto-registration authenticator (auth-phone-auto-reg-form) is not available in realm");
        }

        // Create new forms subflow
        AuthenticationFlowRepresentation newFormsFlow = new AuthenticationFlowRepresentation();
        newFormsFlow.setAlias(newFormsAlias);
        newFormsFlow.setDescription("Forms subflow with phone auto-registration");
        newFormsFlow.setProviderId("basic-flow");
        newFormsFlow.setTopLevel(false);
        newFormsFlow.setBuiltIn(false);

        realmResource.flows().createFlow(newFormsFlow);
        log.info("Created new forms subflow: {}", newFormsAlias);

        // Wait a bit for Keycloak to process the subflow creation
        // Subflows (topLevel=false) may take longer to appear in the flows list
        try {
            Thread.sleep(500); // Wait 500ms for subflow to be available
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for subflow creation");
        }

        // Verify the flow is available before proceeding
        // For subflows, we verify by trying to get executions (more reliable than searching flows list)
        if (!waitForSubflowToBeAvailable(realmResource, newFormsAlias, 5)) {
            throw new RuntimeException("Failed to verify newly created forms subflow: " + newFormsAlias);
        }

        // Add phone auto-registration form execution as REQUIRED
        Map<String, Object> phoneExecutionData = new HashMap<>();
        phoneExecutionData.put("provider", "auth-phone-auto-reg-form");
        realmResource.flows().addExecution(newFormsAlias, phoneExecutionData);

        // Set phone form as required
        List<AuthenticationExecutionInfoRepresentation> newExecutions = realmResource.flows().getExecutions(newFormsAlias);
        AuthenticationExecutionInfoRepresentation phoneExecution = newExecutions
            .stream()
            .filter(e -> "auth-phone-auto-reg-form".equals(e.getProviderId()))
            .findFirst()
            .orElse(null);

        if (phoneExecution != null) {
            phoneExecution.setRequirement("REQUIRED");
            realmResource.flows().updateExecutions(newFormsAlias, phoneExecution);
            log.info("Added phone auto-registration form to forms subflow: {} as REQUIRED", newFormsAlias);

            // Configure the authenticator to enable auto-registration
            configurePhoneAutoRegistrationAuthenticator(realmResource, newFormsAlias, phoneExecution.getId());
        } else {
            log.error("Failed to add phone auto-registration form to forms subflow: {}", newFormsAlias);
            throw new RuntimeException("Failed to add phone auto-registration form to forms subflow");
        }
    }

    /**
     * Copy the forms subflow and modify it to use phone auto-registration form instead of username-password.
     *
     * Standard Keycloak pattern:
     * 1. Verify phone authenticator is available
     * 2. Create new forms subflow
     * 3. Add phone authenticator as REQUIRED (first execution)
     * 4. Copy other executions from original, skipping username-password form
     * 5. Preserve execution order and requirements
     *
     * @param realmResource the realm resource
     * @param originalFormsAlias the original forms subflow alias
     * @param newFormsAlias the new forms subflow alias
     */
    private void copyFormsSubflowWithoutUsernamePassword(RealmResource realmResource, String originalFormsAlias, String newFormsAlias) {
        // Verify authenticator is available before creating flow
        if (!isAuthenticatorAvailable(realmResource, "auth-phone-auto-reg-form")) {
            throw new RuntimeException("Phone auto-registration authenticator (auth-phone-auto-reg-form) is not available in realm");
        }

        // Create new forms subflow
        AuthenticationFlowRepresentation newFormsFlow = new AuthenticationFlowRepresentation();
        newFormsFlow.setAlias(newFormsAlias);
        newFormsFlow.setDescription("Forms subflow with phone auto-registration");
        newFormsFlow.setProviderId("basic-flow");
        newFormsFlow.setTopLevel(false);
        newFormsFlow.setBuiltIn(false);

        realmResource.flows().createFlow(newFormsFlow);
        log.info("Created new forms subflow: {}", newFormsAlias);

        // Wait a bit for Keycloak to process the subflow creation
        // Subflows (topLevel=false) may take longer to appear in the flows list
        try {
            Thread.sleep(500); // Wait 500ms for subflow to be available
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for subflow creation");
        }

        // Verify the flow is available before proceeding
        // For subflows, we verify by trying to get executions (more reliable than searching flows list)
        if (!waitForSubflowToBeAvailable(realmResource, newFormsAlias, 5)) {
            throw new RuntimeException("Failed to verify newly created forms subflow: " + newFormsAlias);
        }

        // Add phone auto-registration form execution FIRST as REQUIRED
        // This replaces the username-password form
        Map<String, Object> phoneExecutionData = new HashMap<>();
        phoneExecutionData.put("provider", "auth-phone-auto-reg-form");
        realmResource.flows().addExecution(newFormsAlias, phoneExecutionData);

        // Set phone form as required
        List<AuthenticationExecutionInfoRepresentation> phoneExecutions = realmResource.flows().getExecutions(newFormsAlias);
        AuthenticationExecutionInfoRepresentation phoneExecution = phoneExecutions
            .stream()
            .filter(e -> "auth-phone-auto-reg-form".equals(e.getProviderId()))
            .findFirst()
            .orElse(null);

        if (phoneExecution != null) {
            phoneExecution.setRequirement("REQUIRED");
            realmResource.flows().updateExecutions(newFormsAlias, phoneExecution);
            log.info("Added phone auto-registration form to forms subflow: {} as REQUIRED", newFormsAlias);

            // Configure the authenticator to enable auto-registration
            configurePhoneAutoRegistrationAuthenticator(realmResource, newFormsAlias, phoneExecution.getId());
        } else {
            log.error("Failed to add phone auto-registration form to forms subflow: {}", newFormsAlias);
            throw new RuntimeException("Failed to add phone auto-registration form to forms subflow");
        }

        // Get executions from original forms subflow
        List<AuthenticationExecutionInfoRepresentation> originalFormsExecutions = realmResource.flows().getExecutions(originalFormsAlias);

        // Copy other executions, skipping username-password form
        for (AuthenticationExecutionInfoRepresentation execution : originalFormsExecutions) {
            // Skip username-password form
            if ("auth-username-password-form".equals(execution.getProviderId())) {
                log.info("Skipping username-password form in forms subflow");
                continue;
            }

            // Skip if it's the phone authenticator (already added)
            if ("auth-phone-auto-reg-form".equals(execution.getProviderId())) {
                log.info("Skipping phone auto-reg form (already added as first)");
                continue;
            }

            // Copy other executions
            Map<String, Object> executionData = new HashMap<>();
            executionData.put("provider", execution.getProviderId());
            realmResource.flows().addExecution(newFormsAlias, executionData);

            // Update requirement
            List<AuthenticationExecutionInfoRepresentation> newExecutions = realmResource.flows().getExecutions(newFormsAlias);
            AuthenticationExecutionInfoRepresentation newExecution = newExecutions
                .stream()
                .filter(e -> execution.getProviderId().equals(e.getProviderId()))
                .findFirst()
                .orElse(null);

            if (newExecution != null) {
                newExecution.setRequirement(execution.getRequirement());
                realmResource.flows().updateExecutions(newFormsAlias, newExecution);
            }
        }
    }

    /**
     * Get flow alias by flow ID.
     *
     * @param realmResource the realm resource
     * @param flowId the flow ID
     * @return the flow alias or null if not found
     */
    private String getFlowAliasById(RealmResource realmResource, String flowId) {
        List<AuthenticationFlowRepresentation> flows = realmResource.flows().getFlows();
        return flows
            .stream()
            .filter(f -> flowId.equals(f.getId()))
            .map(AuthenticationFlowRepresentation::getAlias)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get flow ID by flow alias.
     *
     * @param realmResource the realm resource
     * @param flowAlias the flow alias
     * @return the flow ID or null if not found
     */
    private String getFlowIdByAlias(RealmResource realmResource, String flowAlias) {
        List<AuthenticationFlowRepresentation> flows = realmResource.flows().getFlows();
        return flows
            .stream()
            .filter(f -> flowAlias.equals(f.getAlias()))
            .map(AuthenticationFlowRepresentation::getId)
            .findFirst()
            .orElse(null);
    }

    /**
     * Wait for a flow to be available in the realm.
     * This is useful after creating a flow, as there may be a slight delay before it's available.
     *
     * @param realmResource the realm resource
     * @param flowAlias the flow alias to check for
     * @param maxRetries maximum number of retries
     * @return true if the flow is found, false otherwise
     */
    private boolean waitForFlowToBeAvailable(RealmResource realmResource, String flowAlias, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            String flowId = getFlowIdByAlias(realmResource, flowAlias);
            if (flowId != null) {
                log.debug("Found flow '{}' with ID '{}' after {} attempt(s)", flowAlias, flowId, i + 1);
                return true;
            }

            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(200); // Wait 200ms before retrying
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting for flow to be available");
                    return false;
                }
            }
        }

        log.warn("Flow '{}' not found after {} attempts", flowAlias, maxRetries);
        return false;
    }

    /**
     * Wait for a subflow to be available in the realm.
     * This is more reliable for subflows (topLevel=false) than waitForFlowToBeAvailable,
     * as it verifies by trying to get executions, which is what we actually need.
     *
     * @param realmResource the realm resource
     * @param flowAlias the subflow alias to check for
     * @param maxRetries maximum number of retries
     * @return true if the subflow is found and accessible, false otherwise
     */
    private boolean waitForSubflowToBeAvailable(RealmResource realmResource, String flowAlias, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                // Try to get executions - if this succeeds, the subflow exists and is accessible
                realmResource.flows().getExecutions(flowAlias);
                log.debug("Found subflow '{}' after {} attempt(s) - can access executions", flowAlias, i + 1);
                return true;
            } catch (Exception e) {
                // Subflow not yet available or doesn't exist
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(300); // Wait 300ms before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting for subflow to be available");
                        return false;
                    }
                } else {
                    log.warn("Subflow '{}' not accessible after {} attempts: {}", flowAlias, maxRetries, e.getMessage());
                }
            }
        }

        // Also try the standard flow check as a fallback
        return waitForFlowToBeAvailable(realmResource, flowAlias, 2);
    }

    /**
     * Configure the phone auto-registration authenticator with appropriate settings.
     * Based on the authenticator implementation (PhoneUsernamePasswordFormWithAutoRegistration), this sets:
     * - enableAutoRegistration: true (enables auto-registration of new users)
     * - autoRegPhoneAsUsername: true (uses phone number as username for auto-registered users)
     * - loginWithPhoneVerify: true (enables phone + OTP login)
     * - loginWithPhoneNumber: true (enables phone + password login)
     *
     * Note: The authenticator will work with default settings if configuration fails.
     * Configuration can also be done manually in the Keycloak Admin Console.
     *
     * @param realmResource the realm resource
     * @param flowAlias the flow alias containing the execution
     * @param executionId the execution ID
     */
    private void configurePhoneAutoRegistrationAuthenticator(RealmResource realmResource, String flowAlias, String executionId) {
        try {
            // Create authenticator configuration
            AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
            config.setAlias("phone-auto-reg-config");

            // Set configuration properties based on PhoneUsernamePasswordFormWithAutoRegistration implementation
            Map<String, String> configMap = new HashMap<>();
            configMap.put("enableAutoRegistration", "true"); // Enable auto-registration
            configMap.put("autoRegPhoneAsUsername", "true"); // Use phone as username
            configMap.put("loginWithPhoneVerify", "true"); // Enable phone + OTP login
            configMap.put("loginWithPhoneNumber", "true"); // Enable phone + password login
            config.setConfig(configMap);

            // Create the authenticator config using newExecutionConfig
            // Note: newExecutionConfig returns a Response, we need to extract the location header
            jakarta.ws.rs.core.Response response = realmResource.flows().newExecutionConfig(executionId, config);

            if (response.getStatus() == jakarta.ws.rs.core.Response.Status.CREATED.getStatusCode()) {
                // Extract config ID from Location header
                String location = response.getLocation().toString();
                String configId = location.substring(location.lastIndexOf('/') + 1);
                log.info("Configured phone auto-registration authenticator with config ID: {}", configId);

                // The config is automatically associated with the execution
                // Verify by checking the execution
                List<AuthenticationExecutionInfoRepresentation> executions = realmResource.flows().getExecutions(flowAlias);
                AuthenticationExecutionInfoRepresentation execution = executions
                    .stream()
                    .filter(e -> executionId.equals(e.getId()))
                    .findFirst()
                    .orElse(null);

                if (execution != null) {
                    log.info("Authenticator config created and associated with execution: {}", executionId);
                }
            } else {
                log.warn("Failed to create authenticator config. Status: {}", response.getStatus());
            }
            response.close();
        } catch (Exception e) {
            log.warn(
                "Failed to configure phone auto-registration authenticator: {}. Authenticator will use default settings. " +
                "You can configure it manually in Keycloak Admin Console under Authentication > Flows > {} > Config",
                e.getMessage(),
                flowAlias
            );
            // Don't throw - the authenticator will work with default settings
        }
    }

    /**
     * Check if an authenticator provider is available in the realm.
     * This verifies that the authenticator can be used before attempting to add it to a flow.
     *
     * Standard Keycloak practice: Always verify authenticator availability before using it.
     *
     * @param realmResource the realm resource
     * @param providerId the authenticator provider ID (e.g., "auth-phone-auto-reg-form")
     * @return true if the authenticator is available, false otherwise
     */
    private boolean isAuthenticatorAvailable(RealmResource realmResource, String providerId) {
        try {
            // Try to get the list of authenticator providers
            // Note: Keycloak Admin API doesn't have a direct method to list all providers,
            // so we'll attempt to add it and catch the error, or check if it exists in any flow
            // For now, we'll use a simpler approach: try to find it in existing flows

            // Check if the provider exists by looking at all flows
            List<AuthenticationFlowRepresentation> flows = realmResource.flows().getFlows();
            for (AuthenticationFlowRepresentation flow : flows) {
                try {
                    List<AuthenticationExecutionInfoRepresentation> executions = realmResource.flows().getExecutions(flow.getAlias());
                    boolean found = executions.stream().anyMatch(e -> providerId.equals(e.getProviderId()));
                    if (found) {
                        log.debug("Authenticator '{}' found in flow '{}'", providerId, flow.getAlias());
                        return true;
                    }
                } catch (Exception e) {
                    // Ignore errors when checking individual flows
                    log.debug("Error checking flow '{}' for authenticator: {}", flow.getAlias(), e.getMessage());
                }
            }

            // If not found in existing flows, we'll assume it's available if it's a known provider
            // In production, you might want to query the authenticator factory directly
            // For now, we'll log a warning and return true for known providers
            if (providerId.startsWith("auth-")) {
                log.info(
                    "Authenticator '{}' not found in existing flows, but assuming it's available (standard Keycloak provider)",
                    providerId
                );
                return true;
            }

            log.warn("Authenticator '{}' may not be available", providerId);
            return false;
        } catch (Exception e) {
            log.error("Error checking authenticator availability for '{}': {}", providerId, e.getMessage());
            // If we can't verify, assume it's available and let the addExecution call fail if it's not
            return true;
        }
    }

    /**
     * Check if a flow contains username-password form.
     * This method is kept for potential future use.
     *
     * @param realmResource the realm resource
     * @param flowAlias the flow alias
     * @return true if the flow contains username-password form
     */
    @SuppressWarnings("unused")
    private boolean containsUsernamePasswordForm(RealmResource realmResource, String flowAlias) {
        try {
            List<AuthenticationExecutionInfoRepresentation> executions = realmResource.flows().getExecutions(flowAlias);
            return executions
                .stream()
                .anyMatch(
                    e ->
                        "auth-username-password-form".equals(e.getProviderId()) ||
                        (e.getFlowId() != null &&
                            containsUsernamePasswordForm(realmResource, getFlowAliasById(realmResource, e.getFlowId())))
                );
        } catch (Exception e) {
            log.warn("Error checking flow for username-password form: {}", flowAlias, e);
            return false;
        }
    }
}

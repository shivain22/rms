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
     * @param realmName the realm name
     */
    public void copyAndModifyBrowserFlow(String realmName) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realmName);
            String newFlowAlias = "browser-phone-flow";

            // Get the default browser flow
            List<AuthenticationFlowRepresentation> flows = realmResource.flows().getFlows();
            AuthenticationFlowRepresentation browserFlow = flows
                .stream()
                .filter(f -> "browser".equals(f.getAlias()) && f.isBuiltIn())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Default browser flow not found"));

            log.info("Found default browser flow: {}", browserFlow.getAlias());

            // Copy the browser flow
            AuthenticationFlowRepresentation newFlow = new AuthenticationFlowRepresentation();
            newFlow.setAlias(newFlowAlias);
            newFlow.setDescription("Browser flow with phone auto-registration");
            newFlow.setProviderId("basic-flow");
            newFlow.setTopLevel(true);
            newFlow.setBuiltIn(false);

            realmResource.flows().createFlow(newFlow);
            log.info("Created new flow: {}", newFlowAlias);

            // Get all executions from the original browser flow
            List<AuthenticationExecutionInfoRepresentation> originalExecutions = realmResource.flows().getExecutions("browser");

            // Find the forms subflow (if it exists)
            String formsSubflowId = null;
            String formsSubflowAlias = null;
            for (AuthenticationExecutionInfoRepresentation execution : originalExecutions) {
                if (execution.getFlowId() != null && !execution.getFlowId().isEmpty()) {
                    String subflowAlias = getFlowAliasById(realmResource, execution.getFlowId());
                    if ("forms".equals(subflowAlias)) {
                        formsSubflowId = execution.getFlowId();
                        formsSubflowAlias = subflowAlias;
                        break;
                    }
                }
            }

            // Copy executions from the original browser flow
            for (AuthenticationExecutionInfoRepresentation execution : originalExecutions) {
                // Skip username-password form execution if it's direct
                if ("auth-username-password-form".equals(execution.getProviderId())) {
                    log.info("Skipping username-password form execution");
                    continue;
                }

                // Handle forms subflow specially
                if (execution.getFlowId() != null && execution.getFlowId().equals(formsSubflowId)) {
                    // Create a new forms subflow without username-password form
                    String newFormsSubflowAlias = "forms-phone";
                    copyFormsSubflowWithoutUsernamePassword(realmResource, formsSubflowAlias, newFormsSubflowAlias);

                    // Add the new forms subflow to the browser flow
                    Map<String, Object> executionData = new HashMap<>();
                    executionData.put("provider", newFormsSubflowAlias);
                    realmResource.flows().addExecution(newFlowAlias, executionData);

                    // Update requirement
                    List<AuthenticationExecutionInfoRepresentation> newExecutions = realmResource.flows().getExecutions(newFlowAlias);
                    AuthenticationExecutionInfoRepresentation newExecution = newExecutions
                        .stream()
                        .filter(e -> e.getFlowId() != null && getFlowAliasById(realmResource, e.getFlowId()).equals(newFormsSubflowAlias))
                        .findFirst()
                        .orElse(null);

                    if (newExecution != null) {
                        newExecution.setRequirement(execution.getRequirement());
                        realmResource.flows().updateExecutions(newFlowAlias, newExecution);
                    }
                } else {
                    // Copy other executions as-is
                    Map<String, Object> executionData = new HashMap<>();
                    if (execution.getFlowId() != null && !execution.getFlowId().isEmpty()) {
                        // It's a subflow
                        String subflowAlias = getFlowAliasById(realmResource, execution.getFlowId());
                        executionData.put("provider", subflowAlias);
                    } else {
                        // It's a direct authenticator
                        executionData.put("provider", execution.getProviderId());
                    }

                    realmResource.flows().addExecution(newFlowAlias, executionData);

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

            // Set the new flow as the browser flow
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
     * Copy the forms subflow and modify it to use phone auto-registration form instead of username-password.
     *
     * @param realmResource the realm resource
     * @param originalFormsAlias the original forms subflow alias
     * @param newFormsAlias the new forms subflow alias
     */
    private void copyFormsSubflowWithoutUsernamePassword(RealmResource realmResource, String originalFormsAlias, String newFormsAlias) {
        // Create new forms subflow
        AuthenticationFlowRepresentation newFormsFlow = new AuthenticationFlowRepresentation();
        newFormsFlow.setAlias(newFormsAlias);
        newFormsFlow.setDescription("Forms subflow with phone auto-registration");
        newFormsFlow.setProviderId("basic-flow");
        newFormsFlow.setTopLevel(false);
        newFormsFlow.setBuiltIn(false);

        realmResource.flows().createFlow(newFormsFlow);
        log.info("Created new forms subflow: {}", newFormsAlias);

        // Get executions from original forms subflow
        List<AuthenticationExecutionInfoRepresentation> originalFormsExecutions = realmResource.flows().getExecutions(originalFormsAlias);

        // Copy executions, skipping username-password form
        for (AuthenticationExecutionInfoRepresentation execution : originalFormsExecutions) {
            // Skip username-password form
            if ("auth-username-password-form".equals(execution.getProviderId())) {
                log.info("Skipping username-password form in forms subflow");
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

        // Add phone auto-registration form execution
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
            log.info("Added phone auto-registration form to forms subflow: {}", newFormsAlias);
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
     * Check if a flow contains username-password form.
     *
     * @param realmResource the realm resource
     * @param flowAlias the flow alias
     * @return true if the flow contains username-password form
     */
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

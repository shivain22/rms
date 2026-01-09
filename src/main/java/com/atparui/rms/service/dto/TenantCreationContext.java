package com.atparui.rms.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Context class to track resources created during tenant creation for rollback purposes.
 */
public class TenantCreationContext {

    private boolean tenantSaved = false;
    private boolean databaseCreated = false;
    private boolean realmCreated = false;
    private boolean clientsCreated = false;
    private boolean rolesCreated = false;
    private boolean flowsCreated = false;
    private String tenantId;
    private String tenantKey;
    private Long tenantEntityId;
    private String databaseName;
    private String databaseUser;
    private String realmName;
    private List<String> clientIds = new ArrayList<>();

    public TenantCreationContext(String tenantId, String tenantKey) {
        this.tenantId = tenantId;
        this.tenantKey = tenantKey;
        this.realmName = tenantId + "_realm";
        this.databaseName = "rms_" + tenantKey;
        this.databaseUser = "rms_" + tenantKey;
    }

    public boolean isTenantSaved() {
        return tenantSaved;
    }

    public void setTenantSaved(boolean tenantSaved) {
        this.tenantSaved = tenantSaved;
    }

    public boolean isDatabaseCreated() {
        return databaseCreated;
    }

    public void setDatabaseCreated(boolean databaseCreated) {
        this.databaseCreated = databaseCreated;
    }

    public boolean isRealmCreated() {
        return realmCreated;
    }

    public void setRealmCreated(boolean realmCreated) {
        this.realmCreated = realmCreated;
    }

    public boolean isClientsCreated() {
        return clientsCreated;
    }

    public void setClientsCreated(boolean clientsCreated) {
        this.clientsCreated = clientsCreated;
    }

    public boolean isRolesCreated() {
        return rolesCreated;
    }

    public void setRolesCreated(boolean rolesCreated) {
        this.rolesCreated = rolesCreated;
    }

    public boolean isFlowsCreated() {
        return flowsCreated;
    }

    public void setFlowsCreated(boolean flowsCreated) {
        this.flowsCreated = flowsCreated;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public Long getTenantEntityId() {
        return tenantEntityId;
    }

    public void setTenantEntityId(Long tenantEntityId) {
        this.tenantEntityId = tenantEntityId;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }

    public String getRealmName() {
        return realmName;
    }

    public List<String> getClientIds() {
        return clientIds;
    }

    public void addClientId(String clientId) {
        this.clientIds.add(clientId);
    }

    /**
     * Check if any resources were created that need rollback.
     */
    public boolean hasResourcesToRollback() {
        return tenantSaved || databaseCreated || realmCreated || clientsCreated || rolesCreated || flowsCreated;
    }
}

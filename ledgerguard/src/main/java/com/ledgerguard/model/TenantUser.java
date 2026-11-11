package com.ledgerguard.model;

public class TenantUser {
    private String id;
    private String email;
    private String passwordHash;
    private String role;
    private String orgId;

    public TenantUser() {}

    public TenantUser(String id, String email, String passwordHash, String role, String orgId) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.orgId = orgId;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getOrgId() { return orgId; }
}
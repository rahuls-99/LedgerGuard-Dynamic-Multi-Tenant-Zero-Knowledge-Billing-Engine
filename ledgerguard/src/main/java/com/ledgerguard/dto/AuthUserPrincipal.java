package com.ledgerguard.dto;

public class AuthUserPrincipal {
    private String userId;
    private String orgId;
    private String role;

    public AuthUserPrincipal() {}

    public AuthUserPrincipal(String userId, String orgId, String role) {
        this.userId = userId;
        this.orgId = orgId;
        this.role = role;
    }

    public String getUserId() { return userId; }
    public String getOrgId() { return orgId; }
    public String getRole() { return role; }
}
package com.ledgerguard.dto;

import jakarta.validation.constraints.NotBlank;

public class RealLoginRequest {
    @NotBlank(message = "orgId is required")
    private String orgId;

    @NotBlank(message = "email is required")
    private String email;

    @NotBlank(message = "password is required")
    private String password;

    public RealLoginRequest() {}

    public RealLoginRequest(String orgId, String email, String password) {
        this.orgId = orgId;
        this.email = email;
        this.password = password;
    }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
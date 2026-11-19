package com.ledgerguard.dto;

public class RealLoginResponse {
    private boolean success;
    private String token;
    private AuthUserPrincipal user;

    public RealLoginResponse() {}

    public RealLoginResponse(boolean success, String token, AuthUserPrincipal user) {
        this.success = success;
        this.token = token;
        this.user = user;
    }

    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
    public AuthUserPrincipal getUser() { return user; }
}
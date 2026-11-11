package com.ledgerguard.dto;

public class LoginResponse {
    private boolean success;
    private String token;
    private UserDto user;

    public LoginResponse() {}

    public LoginResponse(boolean success, String token, UserDto user) {
        this.success = success;
        this.token = token;
        this.user = user;
    }

    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
    public UserDto getUser() { return user; }
}
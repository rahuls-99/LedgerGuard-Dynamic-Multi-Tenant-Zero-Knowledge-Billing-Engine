package com.ledgerguard.dto;

public class UserDto {
    private String id;
    private String name;
    private String role;
    private String orgId;
    private String orgName;

    public UserDto() {}

    public UserDto(String id, String name, String role, String orgId, String orgName) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.orgId = orgId;
        this.orgName = orgName;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getOrgId() { return orgId; }
    public String getOrgName() { return orgName; }
}
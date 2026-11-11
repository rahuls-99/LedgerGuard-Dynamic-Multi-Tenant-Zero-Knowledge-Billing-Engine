package com.ledgerguard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDto {
    private String id;
    private String name;
    private String role;
    private String orgId;
    private String orgName;
}
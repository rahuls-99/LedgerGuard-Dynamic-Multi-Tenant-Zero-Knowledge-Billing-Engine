package com.ledgerguard.dto;

import lombok.Data;

@Data
public class TenantCreateRequest {
    private String tenantId;
    private String companyName;
}
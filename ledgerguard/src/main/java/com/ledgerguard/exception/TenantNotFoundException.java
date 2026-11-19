package com.ledgerguard.exception;

import org.springframework.http.HttpStatus;

public class TenantNotFoundException extends ApiException {
    public TenantNotFoundException(String tenantId) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tenant not found: " + tenantId);
    }
}
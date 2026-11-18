package com.ledgerguard.controller;

import com.ledgerguard.dto.TenantCreateRequest;
import com.ledgerguard.entity.Tenant;
import com.ledgerguard.service.TenantProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantProvisionController {

    private final TenantProvisioningService provisioningService;

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody TenantCreateRequest request) {
        Tenant created = provisioningService.provisionNewTenant(request);
        return ResponseEntity.ok(created);
    }
}
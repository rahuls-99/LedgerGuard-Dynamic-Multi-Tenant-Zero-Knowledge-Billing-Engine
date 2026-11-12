package com.ledgerguard.controller;

import com.ledgerguard.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminPing() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "pong (admin access verified)");
        response.put("tenantId", TenantContext.getTenantId());
        return ResponseEntity.ok(response);
    }
}

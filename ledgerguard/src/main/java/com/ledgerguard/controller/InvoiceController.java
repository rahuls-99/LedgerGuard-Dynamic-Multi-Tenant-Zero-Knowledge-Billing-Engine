package com.ledgerguard.controller;

import com.ledgerguard.dto.AuthUserPrincipal;
import com.ledgerguard.entity.Invoice;
import com.ledgerguard.exception.ApiException;
import com.ledgerguard.multitenancy.TenantContext;
import com.ledgerguard.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    private String resolveAndVerifyTenant(AuthUserPrincipal principal, String headerOrgId, String headerTenantId) {
        if (principal == null || principal.getOrgId() == null || principal.getOrgId().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Missing tenant claim in token");
        }
        String tokenOrgId = principal.getOrgId();
        if (headerOrgId != null && !headerOrgId.isBlank() && !tokenOrgId.equalsIgnoreCase(headerOrgId.trim())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Cross-tenant access denied: token belongs to " + tokenOrgId);
        }

        if (headerTenantId != null && !headerTenantId.isBlank()) {
            TenantContext.setTenantId(headerTenantId.trim());
        } else if ("orgA".equalsIgnoreCase(tokenOrgId)) {
            TenantContext.setTenantId("alpha");
        } else if ("orgB".equalsIgnoreCase(tokenOrgId)) {
            TenantContext.setTenantId("beta");
        }

        return tokenOrgId;
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> listInvoices(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(value = "X-Org-Id", required = false) String headerOrgId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String headerTenantId) {
        String orgId = resolveAndVerifyTenant(principal, headerOrgId, headerTenantId);
        return ResponseEntity.ok(invoiceRepository.findAllByOrgId(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(value = "X-Org-Id", required = false) String headerOrgId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String headerTenantId) {
        String orgId = resolveAndVerifyTenant(principal, headerOrgId, headerTenantId);
        Invoice invoice = invoiceRepository.findByIdAndOrgId(id, orgId).orElse(null);
        if (invoice == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Invoice not found in your organization boundary");
        }
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getDashboardStats(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(value = "X-Org-Id", required = false) String headerOrgId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String headerTenantId) {
        String orgId = resolveAndVerifyTenant(principal, headerOrgId, headerTenantId);

        BigDecimal totalRevenue = invoiceRepository.sumPaidRevenueByOrgId(orgId);
        long totalInvoices = invoiceRepository.countByOrgId(orgId);
        long pendingCount = invoiceRepository.countByOrgIdAndStatus(orgId, "PENDING");
        long overdueCount = invoiceRepository.countByOrgIdAndStatus(orgId, "OVERDUE");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "orgId", orgId,
                "totalRevenue", totalRevenue,
                "totalInvoices", totalInvoices,
                "pendingInvoices", pendingCount,
                "overdueInvoices", overdueCount
        ));
    }
}
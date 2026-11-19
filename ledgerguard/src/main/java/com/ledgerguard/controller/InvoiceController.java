package com.ledgerguard.controller;

import com.ledgerguard.dto.AuthUserPrincipal;
import com.ledgerguard.entity.Invoice;
import com.ledgerguard.exception.ApiException;
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

    private String resolveAndVerifyTenant(AuthUserPrincipal principal, String headerOrgId) {
        if (principal == null || principal.getOrgId() == null || principal.getOrgId().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Missing tenant claim in token");
        }
        String tokenOrgId = principal.getOrgId();
        if (headerOrgId != null && !headerOrgId.isBlank() && !tokenOrgId.equals(headerOrgId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Cross-tenant access denied: token belongs to " + tokenOrgId);
        }
        return tokenOrgId;
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> listInvoices(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(value = "X-Org-Id", required = false) String headerOrgId) {
        String orgId = resolveAndVerifyTenant(principal, headerOrgId);
        return ResponseEntity.ok(invoiceRepository.findAllByOrgId(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(value = "X-Org-Id", required = false) String headerOrgId) {
        String orgId = resolveAndVerifyTenant(principal, headerOrgId);
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
            @RequestHeader(value = "X-Org-Id", required = false) String headerOrgId) {
        String orgId = resolveAndVerifyTenant(principal, headerOrgId);

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
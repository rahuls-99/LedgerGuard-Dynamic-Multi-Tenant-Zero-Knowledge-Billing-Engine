package com.ledgerguard.controller;

import com.ledgerguard.model.Account;
import com.ledgerguard.repository.AccountRepository;
import com.ledgerguard.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final JwtUtil jwtUtil;

    public AccountController(AccountRepository accountRepository, JwtUtil jwtUtil) {
        this.accountRepository = accountRepository;
        this.jwtUtil = jwtUtil;
    }

    // Helper: Validate JWT orgId against X-Org-Id header using JwtUtil.extractAllClaims()
    private String validateAndGetOrgId(HttpServletRequest request, String headerOrgId) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            String tokenOrgId = jwtUtil.extractAllClaims(token).get("orgId", String.class);
            if (tokenOrgId == null || !tokenOrgId.equalsIgnoreCase(headerOrgId)) {
                return null;
            }
            return tokenOrgId.toLowerCase().trim();
        } catch (Exception e) {
            return null;
        }
    }

    // 1. CREATE Account
    @PostMapping
    public ResponseEntity<?> createAccount(
            HttpServletRequest request,
            @RequestHeader("X-Org-Id") String headerOrgId,
            @RequestBody Account accountRequest) {

        String orgId = validateAndGetOrgId(request, headerOrgId);
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Cross-tenant access denied"));
        }

        accountRequest.setId(null);
        accountRequest.setOrgId(orgId);
        Account saved = accountRepository.save(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 2. READ All Accounts for Tenant
    @GetMapping
    public ResponseEntity<?> getAllAccounts(
            HttpServletRequest request,
            @RequestHeader("X-Org-Id") String headerOrgId) {

        String orgId = validateAndGetOrgId(request, headerOrgId);
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Cross-tenant access denied"));
        }

        List<Account> accounts = accountRepository.findAllByOrgId(orgId);
        return ResponseEntity.ok(accounts);
    }

    // 3. READ Single Account
    @GetMapping("/{id}")
    public ResponseEntity<?> getAccountById(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestHeader("X-Org-Id") String headerOrgId) {

        String orgId = validateAndGetOrgId(request, headerOrgId);
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Cross-tenant access denied"));
        }

        Optional<Account> account = accountRepository.findByIdAndOrgId(id, orgId);
        return account.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Account not found")));
    }

    // 4. UPDATE Account
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestHeader("X-Org-Id") String headerOrgId,
            @RequestBody Account updateRequest) {

        String orgId = validateAndGetOrgId(request, headerOrgId);
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Cross-tenant access denied"));
        }

        Optional<Account> existingOpt = accountRepository.findByIdAndOrgId(id, orgId);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Account not found"));
        }

        Account existing = existingOpt.get();
        if (updateRequest.getAccountName() != null) existing.setAccountName(updateRequest.getAccountName());
        if (updateRequest.getAccountType() != null) existing.setAccountType(updateRequest.getAccountType());
        if (updateRequest.getBalance() != null) existing.setBalance(updateRequest.getBalance());
        if (updateRequest.getCurrency() != null) existing.setCurrency(updateRequest.getCurrency());
        if (updateRequest.getStatus() != null) existing.setStatus(updateRequest.getStatus());

        return ResponseEntity.ok(accountRepository.save(existing));
    }

    // 5. DELETE Account
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestHeader("X-Org-Id") String headerOrgId) {

        String orgId = validateAndGetOrgId(request, headerOrgId);
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Cross-tenant access denied"));
        }

        Optional<Account> existingOpt = accountRepository.findByIdAndOrgId(id, orgId);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Account not found"));
        }

        accountRepository.delete(existingOpt.get());
        return ResponseEntity.ok(Map.of("success", true, "message", "Account deleted successfully"));
    }
}
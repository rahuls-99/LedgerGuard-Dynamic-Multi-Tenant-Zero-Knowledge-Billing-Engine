package com.ledgerguard.controller;

import com.ledgerguard.dto.MeResponse;
import com.ledgerguard.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getCurrentUser(Authentication auth, HttpServletRequest request) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }

        String email = auth.getName();
        String orgId = (String) request.getAttribute("orgId");
        String userId = (String) request.getAttribute("userId");
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_MEMBER");

        String safeOrgId = (orgId != null) ? orgId : "orgA";
        String orgName = safeOrgId.equalsIgnoreCase("orgA") ? "Acme FinTech (Org A)" : "Globex Corp (Org B)";

        return ResponseEntity.ok(new MeResponse(
                true,
                userId != null ? userId : "usr_" + email.hashCode(),
                email,
                safeOrgId,
                orgName,
                role,
                "ENTERPRISE_ZK"
        ));
    }
}
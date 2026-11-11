package com.ledgerguard.controller;

import com.ledgerguard.dto.AuthUserPrincipal;
import com.ledgerguard.dto.ErrorResponse;
import com.ledgerguard.dto.RealLoginRequest;
import com.ledgerguard.service.RealAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/real-auth")
public class RealAuthController {

    private final RealAuthService realAuthService;

    public RealAuthController(RealAuthService realAuthService) {
        this.realAuthService = realAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody RealLoginRequest request) {
        return realAuthService.login(request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(false, "Invalid credentials")));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(false, "Unauthorized"));
        }
        return ResponseEntity.ok(principal);
    }
}
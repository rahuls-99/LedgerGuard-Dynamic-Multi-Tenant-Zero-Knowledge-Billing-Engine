package com.ledgerguard.controller;

import com.ledgerguard.dto.AuthUserPrincipal;
import com.ledgerguard.dto.RealLoginRequest;
import com.ledgerguard.service.RealAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/real-auth")
public class RealAuthController {

    private final RealAuthService realAuthService;

    public RealAuthController(RealAuthService realAuthService) {
        this.realAuthService = realAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody RealLoginRequest request) {
        var res = realAuthService.login(request);
        if (res.isPresent()) {
            return ResponseEntity.ok(res.get());
        }
        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid credentials"));
    }

    @GetMapping("/me")
    public ResponseEntity getCurrentUser(@AuthenticationPrincipal AuthUserPrincipal principal) {
        Map response = new HashMap();
        response.put("userId", principal.getUserId());
        response.put("orgId", principal.getOrgId());
        response.put("role", principal.getRole());
        return ResponseEntity.ok(response);
    }
}

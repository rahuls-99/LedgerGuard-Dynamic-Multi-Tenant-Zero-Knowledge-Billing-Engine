package com.ledgerguard.service;

import com.ledgerguard.model.TenantUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TenantConnectionProvider {

    private final PasswordEncoder passwordEncoder;

    public TenantConnectionProvider(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<TenantUser> findUserInTenantDb(String orgId, String email) {
        if ("orgA".equalsIgnoreCase(orgId) && "admin@orga.com".equalsIgnoreCase(email)) {
            String hashedPassword = passwordEncoder.encode("123456");
            return Optional.of(new TenantUser("u101", "admin@orga.com", hashedPassword, "admin", "orgA"));
        }
        return Optional.empty();
    }
}
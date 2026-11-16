package com.ledgerguard.service;

import com.ledgerguard.model.TenantUser;
import com.ledgerguard.multitenancy.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TenantConnectionProvider {

    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public TenantConnectionProvider(PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TenantUser> findUserInTenantDb(String orgId, String email) {
        if (orgId == null || orgId.isBlank() || email == null || email.isBlank()) {
            return Optional.empty();
        }

        String normalizedOrgId = orgId.toLowerCase().trim();

        // Backwards compatibility for demo orgA
        if ("orga".equals(normalizedOrgId) && "admin@orga.com".equalsIgnoreCase(email.trim())) {
            String hashedPassword = passwordEncoder.encode("123456");
            return Optional.of(new TenantUser("u101", "admin@orga.com", hashedPassword, "admin", "orgA"));
        }

        // Dynamic lookup from db_tenant_<orgId>.users
        if (!normalizedOrgId.matches("^[a-z0-9_]+$")) {
            return Optional.empty();
        }

        String dbName = "db_tenant_" + normalizedOrgId;
        String sql = "SELECT id, email, password, role FROM `" + dbName + "`.users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try {
            TenantContext.clear();
            List<TenantUser> users = jdbcTemplate.query(sql, (rs, rowNum) -> {
                String dbPass = rs.getString("password");
                // यदि DB में पासवर्ड BCrypt ($2a$...) फॉर्मेट में नहीं है, तो उसे भी वैलिड मान लें
                String validHash = (dbPass != null && dbPass.startsWith("$2"))
                        ? dbPass
                        : passwordEncoder.encode(dbPass);
                return new TenantUser(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("email"),
                        validHash,
                        rs.getString("role"),
                        normalizedOrgId
                );
            }, email.trim());

            return users.stream().findFirst();
        } catch (Exception e) {
            System.err.println("DB Login Error for " + normalizedOrgId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
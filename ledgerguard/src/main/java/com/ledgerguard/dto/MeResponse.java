package com.ledgerguard.dto;

public record MeResponse(
        boolean success,
        String userId,
        String email,
        String orgId,
        String orgName,
        String role,
        String planTier
) {}
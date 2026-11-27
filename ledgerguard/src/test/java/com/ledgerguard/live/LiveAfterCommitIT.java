package com.ledgerguard.live;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LiveAfterCommitIT {

    @Test
    void payloadContainsOnlyAllowedKeys() {
        TransactionFinalizedPayload payload = TransactionFinalizedPayload.of(
                "tx-100",
                "CHARGE",
                "SUCCESS",
                new BigDecimal("49.50"),
                "USD",
                Instant.parse("2026-11-27T12:00:00Z")
        );
        Set<String> allowed = Set.of("transactionId", "type", "status", "amount", "currency", "occurredAt");
        assertEquals(allowed, payload.toMap().keySet());
        assertFalse(payload.toMap().containsKey("walletId"));
        assertFalse(payload.toMap().containsKey("idempotencyKey"));
    }
}
package com.ledgerguard.live;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record TransactionFinalizedPayload(
        String transactionId,
        String type,
        String status,
        String amount,
        String currency,
        Instant occurredAt
) {
    public static TransactionFinalizedPayload of(
            String transactionId,
            String type,
            String status,
            BigDecimal amount,
            String currency,
            Instant occurredAt
    ) {
        return new TransactionFinalizedPayload(
                transactionId,
                type,
                status,
                amount != null ? amount.toPlainString() : "0.00",
                currency,
                occurredAt != null ? occurredAt : Instant.now()
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("transactionId", transactionId);
        map.put("type", type);
        map.put("status", status);
        map.put("amount", amount);
        map.put("currency", currency);
        map.put("occurredAt", occurredAt.toString());
        return map;
    }
}
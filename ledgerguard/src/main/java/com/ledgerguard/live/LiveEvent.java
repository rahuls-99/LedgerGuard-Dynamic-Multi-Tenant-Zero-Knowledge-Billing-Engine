package com.ledgerguard.live;

import java.time.Instant;
import java.util.Map;

public record LiveEvent(
        String id,
        String type,
        String tenantId,
        Instant timestamp,
        Map<String, Object> data
) {
    public static LiveEvent control(String id, String type, Map<String, Object> data) {
        return new LiveEvent(id, type, null, Instant.now(), data);
    }

    public static LiveEvent tenantEvent(String id, String type, String tenantId, Map<String, Object> data) {
        return new LiveEvent(id, type, tenantId, Instant.now(), data);
    }
}
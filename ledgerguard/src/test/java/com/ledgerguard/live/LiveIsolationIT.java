package com.ledgerguard.live;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LiveIsolationIT {

    @Test
    void controlFramesContainNoTenantField() {
        LiveEvent hb = LiveEvent.control(null, "heartbeat", Map.of("serverTime", "2026-11-27T00:00:00Z"));
        assertFalse(hb.data().containsKey("tenantId"));
        assertEquals("heartbeat", hb.type());
    }
}
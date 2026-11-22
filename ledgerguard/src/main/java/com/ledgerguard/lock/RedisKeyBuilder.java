package com.ledgerguard.lock;

import com.ledgerguard.multitenancy.TenantContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RedisKeyBuilder {

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    public String walletLock(String walletId) {
        return "lg:" + tenant() + ":lock:wallet:" + safe(walletId);
    }

    public String idempotencyLock(String key) {
        return "lg:" + tenant() + ":lock:idem:" + safe(key);
    }

    private String tenant() {
        String org = TenantContext.getTenantId();
        if (org == null || org.isBlank()) {
            throw new IllegalStateException("No tenant in context");
        }
        return safe(org);
    }

    private String safe(String v) {
        if (v == null || !SAFE.matcher(v).matches()) {
            throw new IllegalArgumentException("Invalid key part: " + v);
        }
        return v;
    }
}
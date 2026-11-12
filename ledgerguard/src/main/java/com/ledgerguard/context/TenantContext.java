package com.ledgerguard.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    public static void setTenantId(String tenantId) {
        log.debug("Setting TenantContext to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId != null) {
            log.info("Clearing TenantContext for tenant: {}", tenantId);
        }
        CURRENT_TENANT.remove();
    }
}
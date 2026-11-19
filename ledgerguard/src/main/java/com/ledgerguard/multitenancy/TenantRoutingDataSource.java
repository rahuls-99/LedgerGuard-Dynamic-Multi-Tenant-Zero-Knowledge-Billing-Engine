package com.ledgerguard.multitenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        // अगर TenantContext खाली है, तो "MASTER" DB पर जाएं, वरना उस Tenant के DB पर
        String tenantId = TenantContext.getTenantId();
        return (tenantId != null) ? tenantId : "MASTER";
    }
}
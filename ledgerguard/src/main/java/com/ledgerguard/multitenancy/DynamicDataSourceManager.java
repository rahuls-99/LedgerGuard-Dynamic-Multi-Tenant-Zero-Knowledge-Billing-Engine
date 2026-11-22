package com.ledgerguard.multitenancy;

import com.ledgerguard.entity.Tenant;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicDataSourceManager {

    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
    private TenantRoutingDataSource routingDataSource;

    public void init(DataSource masterDataSource) {
        targetDataSources.put("MASTER", masterDataSource);
        routingDataSource = new TenantRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();
    }

    public DataSource getRoutingDataSource() {
        return routingDataSource;
    }

    // नए या मौजूदा Tenant के लिए डायनामिक HikariCP Pool बनाना
    public void addTenantDataSource(Tenant tenant) {
        if (targetDataSources.containsKey(tenant.getTenantId())) {
            return;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(tenant.getDbUrl());
        config.setUsername(tenant.getDbUsername());
        config.setPassword(tenant.getDbPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(5); // हर Tenant के लिए अधिकतम 5 कनेक्शन
        config.setPoolName("HikariPool-" + tenant.getTenantId());

        HikariDataSource tenantDs = new HikariDataSource(config);
        targetDataSources.put(tenant.getTenantId(), tenantDs);
        routingDataSource.afterPropertiesSet(); // Router को अपडेट करें
    }
}
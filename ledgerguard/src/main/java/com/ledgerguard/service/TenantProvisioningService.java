package com.ledgerguard.service;

import com.ledgerguard.dto.TenantCreateRequest;
import com.ledgerguard.entity.Tenant;
import com.ledgerguard.multitenancy.DynamicDataSourceManager;
import com.ledgerguard.multitenancy.TenantContext;
import com.ledgerguard.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final TenantRepository tenantRepository;
    private final DynamicDataSourceManager dataSourceManager;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @PostConstruct
    public void loadExistingTenants() {
        TenantContext.clear();
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            try {
                // Ensure database and schema exist even if dropped manually
                jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS `" + tenant.getDbName() + "`");
                runFlywayMigration(tenant.getDbUrl(), tenant.getDbUsername(), tenant.getDbPassword());
                dataSourceManager.addTenantDataSource(tenant);
                log.info("Loaded tenant datasource: {}", tenant.getTenantId());
            } catch (Exception e) {
                log.error("Failed to initialize tenant '{}' on startup: {}", tenant.getTenantId(), e.getMessage());
            }
        }
    }

    public Tenant provisionNewTenant(TenantCreateRequest request) {
        if (request == null || request.getTenantId() == null || request.getTenantId().isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }

        String tenantId = request.getTenantId().toLowerCase().trim();
        if (!tenantId.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Tenant ID can only contain lowercase letters, numbers, and underscores");
        }

        TenantContext.clear();
        if (tenantRepository.findByTenantId(tenantId).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists: " + tenantId);
        }

        String dbName = "db_tenant_" + tenantId;
        String dbUrl = "jdbc:mysql://localhost:3306/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
        runFlywayMigration(dbUrl, dbUsername, dbPassword);

        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setCompanyName(request.getCompanyName().trim());
        tenant.setDbName(dbName);
        tenant.setDbUrl(dbUrl);
        tenant.setDbUsername(dbUsername);
        tenant.setDbPassword(dbPassword);
        Tenant savedTenant = tenantRepository.save(tenant);

        dataSourceManager.addTenantDataSource(savedTenant);
        log.info("Successfully provisioned new tenant: {} -> {}", tenantId, dbName);
        return savedTenant;
    }

    private void runFlywayMigration(String url, String username, String password) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration/tenant")
                .load();
        flyway.repair();
        flyway.migrate();
    }
}
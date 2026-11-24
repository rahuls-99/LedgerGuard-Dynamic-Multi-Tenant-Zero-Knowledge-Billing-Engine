package com.ledgerguard;

import com.ledgerguard.entity.Invoice;
import com.ledgerguard.model.TenantUser;
import com.ledgerguard.repository.InvoiceRepository;
import com.ledgerguard.service.TenantConnectionProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConnectionManagerTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TenantConnectionProvider tenantConnectionProvider;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    @DisplayName("Should establish active connection to ledgerguard MySQL database")
    void shouldConnectToMySqlDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.isValid(2), "MySQL connection must be valid");
            assertEquals("ledgerguard", connection.getCatalog());
        }
    }

    @Test
    @DisplayName("Should resolve valid admin user for orgA")
    void shouldResolveValidTenantUser() {
        Optional<TenantUser> user = tenantConnectionProvider.findUserInTenantDb("orgA", "admin@orga.com");

        assertTrue(user.isPresent());
        assertEquals("u101", user.get().getId());
        assertEquals("orgA", user.get().getOrgId());
        assertEquals("admin", user.get().getRole());
    }

    @Test
    @DisplayName("Should reject mismatched orgId or unknown user")
    void shouldRejectInvalidTenantOrUser() {
        assertTrue(tenantConnectionProvider.findUserInTenantDb("orgB", "admin@orga.com").isEmpty());
        assertTrue(tenantConnectionProvider.findUserInTenantDb("unknownOrg", "test@test.com").isEmpty());
    }

    @Test
    @DisplayName("Should isolate seeded invoice records between orgA and orgB")
    void shouldIsolateSeededDataByOrgId() {
        List<Invoice> orgAInvoices = invoiceRepository.findAllByOrgId("orgA");
        List<Invoice> orgBInvoices = invoiceRepository.findAllByOrgId("orgB");

        assertEquals(4, orgAInvoices.size());
        assertEquals(2, orgBInvoices.size());

        assertTrue(orgAInvoices.stream().allMatch(inv -> "orgA".equals(inv.getOrgId())));
        assertTrue(orgBInvoices.stream().allMatch(inv -> "orgB".equals(inv.getOrgId())));
    }
}
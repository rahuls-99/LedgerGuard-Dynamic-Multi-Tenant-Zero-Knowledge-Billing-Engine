package com.ledgerguard.ledger;
import com.ledgerguard.multitenancy.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
@Component
public class LedgerInvariants {
    private final JdbcTemplate jdbcTemplate;
    public LedgerInvariants(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    public void assertAll(String tenant) {
        String prev = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenant);
            var unbalanced = jdbcTemplate.queryForList(
                    "SELECT transaction_id, " +
                    "SUM(CASE WHEN direction='DEBIT' THEN amount ELSE 0 END) AS debits, " +
                    "SUM(CASE WHEN direction='CREDIT' THEN amount ELSE 0 END) AS credits " +
                    "FROM billing_ledger_entries GROUP BY transaction_id HAVING debits <> credits");
            assertTrue(unbalanced.isEmpty(), "Unbalanced ledger entries found: " + unbalanced);
            Integer negativeWallets = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM billing_wallets WHERE is_system = 0 AND balance < 0", Integer.class);
            assertEquals(0, negativeWallets, "Negative customer wallet balance detected");
            Integer invalidEntries = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM billing_ledger_entries le " +
                    "JOIN billing_transactions t ON le.transaction_id = t.transaction_id " +
                    "WHERE t.status IN ('FAILED', 'ROLLED_BACK')", Integer.class);
            assertEquals(0, invalidEntries, "Ledger entries found for non-SUCCESS transactions");
            var badSuccessCount = jdbcTemplate.queryForList(
                    "SELECT t.transaction_id, COUNT(le.id) AS cnt " +
                    "FROM billing_transactions t LEFT JOIN billing_ledger_entries le ON t.transaction_id = le.transaction_id " +
                    "WHERE t.status = 'SUCCESS' GROUP BY t.transaction_id HAVING cnt <> 2");
            assertTrue(badSuccessCount.isEmpty(), "SUCCESS transactions without exactly 2 entries: " + badSuccessCount);
            BigDecimal netWalletSum = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(balance), 0.00) FROM billing_wallets", BigDecimal.class);
            assertNotNull(netWalletSum);
        } finally {
            if (prev != null) TenantContext.setTenantId(prev);
            else TenantContext.clear();
        }
    }
}

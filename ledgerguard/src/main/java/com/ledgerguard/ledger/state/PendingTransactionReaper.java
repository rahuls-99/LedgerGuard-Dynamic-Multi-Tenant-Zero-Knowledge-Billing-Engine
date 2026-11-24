package com.ledgerguard.ledger.state;
import com.ledgerguard.lock.LockHandle;
import com.ledgerguard.lock.LockService;
import com.ledgerguard.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
@Component
public class PendingTransactionReaper {
    public static final String REAPER_LOCK_KEY = "lg:system:lock:reaper";
    public static final Duration REAPER_LOCK_TTL = Duration.ofSeconds(55);
    private static final Logger log = LoggerFactory.getLogger(PendingTransactionReaper.class);
    private static final String RECOVER_SQL = "UPDATE billing_transactions t SET t.status = 'ROLLED_BACK', t.failure_reason = 'STUCK_PENDING_RECOVERED' WHERE t.status = 'PENDING' AND t.created_at < NOW(3) - INTERVAL 45 SECOND AND NOT EXISTS (SELECT 1 FROM billing_ledger_entries le WHERE le.transaction_id = t.transaction_id)";
    private static final String FIND_ORPHAN_SQL = "SELECT t.transaction_id FROM billing_transactions t WHERE t.status = 'PENDING' AND t.created_at < NOW(3) - INTERVAL 45 SECOND AND EXISTS (SELECT 1 FROM billing_ledger_entries le WHERE le.transaction_id = t.transaction_id)";
    private final LockService lockService;
    private final Supplier<List<String>> activeTenantsSupplier;
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    public PendingTransactionReaper(LockService lockService, Supplier<List<String>> activeTenantsSupplier, DataSource dataSource) {
        this.lockService = lockService;
        this.activeTenantsSupplier = activeTenantsSupplier;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    @Scheduled(fixedDelayString = "${ledgerguard.reaper.fixed-delay-ms:10000}")
    public void reapStuckPendingTransactions() {
        Optional<LockHandle> lockOpt = lockService.tryAcquire(REAPER_LOCK_KEY, REAPER_LOCK_TTL, Duration.ZERO);
        if (lockOpt.isEmpty()) return;
        LockHandle lockHandle = lockOpt.get();
        try {
            for (String tenantId : activeTenantsSupplier.get()) {
                try {
                    TenantContext.setTenantId(tenantId);
                    int recovered = jdbcTemplate.update(RECOVER_SQL);
                    List<String> orphans = jdbcTemplate.queryForList(FIND_ORPHAN_SQL, String.class);
                    for (String txId : orphans) log.error("ALERT: Stuck PENDING has entries tenant={} txId={}", tenantId, txId);
                    log.info("Reaper tenant={} recovered={}", tenantId, recovered);
                } catch (RuntimeException ex) {
                    log.error("Reaper failed for tenant={}", tenantId, ex);
                } finally { TenantContext.clear(); }
            }
        } finally { lockService.release(lockHandle); }
    }
    @Configuration
    static class ReaperTenantsConfig {
        @Bean Supplier<List<String>> activeTenantsSupplier() { return () -> List.of("alpha", "beta"); }
    }
}

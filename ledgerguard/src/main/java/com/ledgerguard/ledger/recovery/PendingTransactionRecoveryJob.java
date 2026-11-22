package com.ledgerguard.ledger.recovery;
import com.ledgerguard.entity.Tenant;
import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import com.ledgerguard.ledger.repository.WalletLedgerEntryRepository;
import com.ledgerguard.ledger.state.FailureCode;
import com.ledgerguard.ledger.state.TransactionStateService;
import com.ledgerguard.multitenancy.TenantContext;
import com.ledgerguard.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
@Component
public class PendingTransactionRecoveryJob {
    private static final Logger log = LoggerFactory.getLogger(PendingTransactionRecoveryJob.class);
    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(2);
    private final TenantRepository tenantRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final WalletLedgerEntryRepository entryRepository;
    private final TransactionStateService stateService;
    public PendingTransactionRecoveryJob(TenantRepository tenantRepository,
                                         LedgerTransactionRepository transactionRepository,
                                         WalletLedgerEntryRepository entryRepository,
                                         TransactionStateService stateService) {
        this.tenantRepository = tenantRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.stateService = stateService;
    }
    @Scheduled(fixedDelayString = "${ledgerguard.recovery.interval-ms:60000}")
    public void recoverAllTenants() {
        TenantContext.clear();
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            recoverTenant(tenant.getTenantId(), STUCK_THRESHOLD);
        }
    }
    public int recoverTenant(String tenantId, Duration threshold) {
        String previousTenant = TenantContext.getTenantId();
        int recovered = 0;
        try {
            TenantContext.setTenantId(tenantId);
            Instant cutoff = Instant.now().minus(threshold);
            List<LedgerTransaction> stuck = transactionRepository.findStuckPendingBefore(cutoff);
            for (LedgerTransaction tx : stuck) {
                int entryCount = entryRepository.findByTransactionId(tx.getTransactionId()).size();
                if (entryCount == 0) {
                    boolean transitioned = stateService.markRolledBack(tx.getTransactionId(), FailureCode.STUCK_PENDING_RECOVERED);
                    if (transitioned) {
                        recovered++;
                        log.warn("Recovered stuck PENDING transaction txId={} tenant={} -> ROLLED_BACK", tx.getTransactionId(), tenantId);
                    }
                }
            }
            return recovered;
        } finally {
            if (previousTenant != null) TenantContext.setTenantId(previousTenant);
            else TenantContext.clear();
        }
    }
}
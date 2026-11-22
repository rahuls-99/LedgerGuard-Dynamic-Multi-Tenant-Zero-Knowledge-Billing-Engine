package com.ledgerguard.ledger.state;
import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
@Service
public class TransactionStateService {
    private static final Logger log = LoggerFactory.getLogger(TransactionStateService.class);
    private final TransactionStateWriter writer;
    private final LedgerTransactionRepository transactionRepository;
    public TransactionStateService(TransactionStateWriter writer, LedgerTransactionRepository transactionRepository) {
        this.writer = writer; this.transactionRepository = transactionRepository;
    }
    public LedgerTransaction insertPending(LedgerTransaction tx) { return writer.savePendingInNewTx(tx); }
    public boolean markFailed(String transactionId, FailureCode code) { return transition(transactionId, TransactionStatus.FAILED, code.name()); }
    public boolean markRolledBack(String transactionId, FailureCode code) { return transition(transactionId, TransactionStatus.ROLLED_BACK, code.name()); }
    public boolean discardPending(String transactionId) {
        int rows = writer.deletePendingInNewTx(transactionId);
        if (rows == 0) { log.info("discardPending skipped: {} not PENDING", transactionId); return false; }
        return true;
    }
    private boolean transition(String transactionId, TransactionStatus target, String failureCode) {
        transactionRepository.findByTransactionId(transactionId).ifPresent(existing -> {
            if (!existing.getStatus().canTransitionTo(target)) {
                log.warn("Invalid transition ignored: txId={} current={} target={}", transactionId, existing.getStatus(), target);
            }
        });
        int rows = writer.updateFromPendingInNewTx(transactionId, target, failureCode);
        if (rows == 0) { log.warn("0 rows affected for txId={} target={}", transactionId, target); return false; }
        return true;
    }
}

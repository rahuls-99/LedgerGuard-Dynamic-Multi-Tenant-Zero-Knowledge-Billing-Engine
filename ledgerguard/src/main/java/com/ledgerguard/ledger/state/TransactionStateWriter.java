package com.ledgerguard.ledger.state;
import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Component
public class TransactionStateWriter {
    private final LedgerTransactionRepository transactionRepository;
    public TransactionStateWriter(LedgerTransactionRepository transactionRepository) { this.transactionRepository = transactionRepository; }
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public LedgerTransaction savePendingInNewTx(LedgerTransaction tx) {
        tx.setStatus(TransactionStatus.PENDING);
        tx.setFailureReason(null);
        return transactionRepository.saveAndFlush(tx);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateFromPendingInNewTx(String transactionId, TransactionStatus targetStatus, String failureCode) {
        return transactionRepository.transitionFromPending(transactionId, targetStatus, failureCode);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int deletePendingInNewTx(String transactionId) {
        return transactionRepository.deleteIfPending(transactionId);
    }
}

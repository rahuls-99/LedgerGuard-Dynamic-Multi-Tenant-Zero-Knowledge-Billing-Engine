package com.ledgerguard.ledger.api;

import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.entity.WalletLedgerEntry;
import com.ledgerguard.ledger.repository.WalletLedgerEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ReplayResponseBuilder {

    private final WalletLedgerEntryRepository entryRepository;
    private final OutcomeMapper outcomeMapper;

    public ReplayResponseBuilder(WalletLedgerEntryRepository entryRepository, OutcomeMapper outcomeMapper) {
        this.entryRepository = entryRepository;
        this.outcomeMapper = outcomeMapper;
    }

    public OutcomeMapper.MappedOutcome build(LedgerTransaction storedTx) {
        BigDecimal balanceAfter = null;
        if (storedTx.getStatus() == TransactionStatus.SUCCESS) {
            List<WalletLedgerEntry> entries = entryRepository.findByTransactionId(storedTx.getTransactionId());
            balanceAfter = entries.stream().filter(e -> storedTx.getWalletId().equals(e.getWalletId())).map(WalletLedgerEntry::getBalanceAfter).findFirst().orElse(null);
        }
        return outcomeMapper.replay(storedTx, balanceAfter);
    }

    public ResponseEntity<Map<String, Object>> buildResponseEntity(LedgerTransaction storedTx) {
        return build(storedTx).response();
    }
}

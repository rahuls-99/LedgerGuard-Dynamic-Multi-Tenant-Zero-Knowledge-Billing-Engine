package com.ledgerguard.ledger.posting;
import com.ledgerguard.ledger.entity.TransactionType;
import java.math.BigDecimal;
public record PostingCommand(String transactionId, TransactionType type, String walletId, BigDecimal amount, String currency) {}

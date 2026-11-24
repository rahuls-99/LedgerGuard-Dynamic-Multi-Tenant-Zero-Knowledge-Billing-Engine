package com.ledgerguard.ledger.posting;
import com.ledgerguard.ledger.entity.TransactionStatus;
import java.math.BigDecimal;
public record PostingResult(TransactionStatus status, String failureCode, BigDecimal balanceAfter) {}

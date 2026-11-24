package com.ledgerguard.ledger.api;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class LedgerApiDtos {
    private LedgerApiDtos() {}
    public record CreateWalletRequest(
            @NotBlank String walletId,
            @NotBlank String ownerId,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @DecimalMin("0.00") BigDecimal initialBalance
    ) {}
    public record WalletResponse(
            String walletId,
            String ownerId,
            String currency,
            BigDecimal balance,
            boolean system,
            Instant updatedAt
    ) {}
    public record PostLedgerRequest(
            @NotNull TransactionType type,
            @NotBlank String walletId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency
    ) {}
    public record PostLedgerResponse(
            String transactionId,
            TransactionStatus status,
            String failureCode,
            BigDecimal balanceAfter,
            boolean idempotentReplay
    ) {}
    public record LedgerEntryView(
            String entryId,
            String transactionId,
            String direction,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Instant createdAt
    ) {}
    public record WalletStatementResponse(
            WalletResponse wallet,
            List<LedgerEntryView> entries
    ) {}
}
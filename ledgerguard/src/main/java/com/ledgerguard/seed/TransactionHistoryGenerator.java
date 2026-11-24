package com.ledgerguard.seed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TransactionHistoryGenerator {

    public record SeedTx(
            String transactionId,
            String walletId,
            String type,
            String status,
            BigDecimal amount,
            String failureReason,
            Instant createdAt,
            BigDecimal balanceAfter
    ) {}

    public record HistoryResult(
            List<SeedTx> transactions,
            Map<String, BigDecimal> walletBalances,
            BigDecimal systemWalletBalance
    ) {}

    public HistoryResult generate90Days(
            String tenantPrefix,
            LocalDate today,
            int targetCount,
            List<String> walletIds,
            Random random
    ) {
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        for (String w : walletIds) {
            balances.put(w, new BigDecimal("50000.0000"));
        }
        BigDecimal systemBalance = new BigDecimal("1000000.0000");

        List<SeedTx> txs = new ArrayList<>(targetCount);
        LocalDate startDate = today.minusDays(89);

        for (int idx = 1; idx <= targetCount; idx++) {
            long dayOffset = Math.min(89L, ((long) (idx - 1) * 90L) / Math.max(1, targetCount));
            LocalDate txDate = startDate.plusDays(dayOffset);
            Instant createdAt = txDate.atStartOfDay(ZoneOffset.UTC)
                    .plusSeconds(random.nextInt(86399))
                    .toInstant();

            String walletId = walletIds.get(idx % walletIds.size());
            double roll = random.nextDouble();
            String type = roll < 0.80 ? "CHARGE" : (roll < 0.92 ? "TOPUP" : "REFUND");

            BigDecimal amount = BigDecimal.valueOf(100.0 + random.nextDouble() * 1400.0)
                    .setScale(2, RoundingMode.HALF_UP)
                    .setScale(4, RoundingMode.HALF_UP);

            String txId = String.format("TXN-SEED-%s-%06d", tenantPrefix, idx);
            BigDecimal currentBal = balances.get(walletId);

            if ("TOPUP".equals(type) || "REFUND".equals(type)) {
                BigDecimal nextBal = currentBal.add(amount);
                systemBalance = systemBalance.subtract(amount);
                balances.put(walletId, nextBal);
                txs.add(new SeedTx(txId, walletId, type, "SUCCESS", amount, null, createdAt, nextBal));
            } else {
                double statusRoll = random.nextDouble();
                if (statusRoll < 0.03) {
                    txs.add(new SeedTx(txId, walletId, type, "ROLLED_BACK", amount, "SYSTEM_ERROR", createdAt, currentBal));
                } else if (currentBal.compareTo(amount) < 0) {
                    txs.add(new SeedTx(txId, walletId, type, "FAILED", amount, "INSUFFICIENT_FUNDS", createdAt, currentBal));
                } else {
                    BigDecimal nextBal = currentBal.subtract(amount);
                    systemBalance = systemBalance.add(amount);
                    balances.put(walletId, nextBal);
                    txs.add(new SeedTx(txId, walletId, type, "SUCCESS", amount, null, createdAt, nextBal));
                }
            }
        }

        return new HistoryResult(txs, balances, systemBalance);
    }
}
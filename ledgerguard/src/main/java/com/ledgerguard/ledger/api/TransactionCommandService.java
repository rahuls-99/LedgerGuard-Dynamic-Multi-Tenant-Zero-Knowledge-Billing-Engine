package com.ledgerguard.ledger.api;

import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.entity.TransactionType;
import com.ledgerguard.lock.LockHandle;
import com.ledgerguard.lock.LockService;
import com.ledgerguard.lock.RedisKeyBuilder;
import com.ledgerguard.ledger.posting.LedgerPostingService;
import com.ledgerguard.ledger.posting.PostingCommand;
import com.ledgerguard.ledger.posting.PostingResult;
import com.ledgerguard.ledger.posting.WalletNotFoundException;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import com.ledgerguard.ledger.state.FailureCode;
import com.ledgerguard.ledger.state.TransactionStateService;
import com.ledgerguard.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionCommandService {

    private static final Logger log = LoggerFactory.getLogger(TransactionCommandService.class);
    private static final Duration IDEM_LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration IDEM_LOCK_WAIT = Duration.ofSeconds(3);
    private static final Duration WALLET_LOCK_TTL = Duration.ofSeconds(20);
    private static final Duration WALLET_LOCK_WAIT = Duration.ofSeconds(3);

    private final LockService lockService;
    private final RedisKeyBuilder keyBuilder;
    private final LedgerTransactionRepository transactionRepository;
    private final TransactionStateService stateService;
    private final LedgerPostingService postingService;
    private final ReplayResponseBuilder replayResponseBuilder;
    private final OutcomeMapper outcomeMapper;

    public TransactionCommandService(LockService lockService,
                                     RedisKeyBuilder keyBuilder,
                                     LedgerTransactionRepository transactionRepository,
                                     TransactionStateService stateService,
                                     LedgerPostingService postingService,
                                     ReplayResponseBuilder replayResponseBuilder,
                                     OutcomeMapper outcomeMapper) {
        this.lockService = lockService;
        this.keyBuilder = keyBuilder;
        this.transactionRepository = transactionRepository;
        this.stateService = stateService;
        this.postingService = postingService;
        this.replayResponseBuilder = replayResponseBuilder;
        this.outcomeMapper = outcomeMapper;
    }

    public ResponseEntity<Map<String, Object>> create(String idempotencyKey,
                                                      TransactionType type,
                                                      String walletId,
                                                      BigDecimal amount,
                                                      String currency) {
        long startNs = System.nanoTime();
        String tenant = TenantContext.getTenantId();
        String cleanWalletId = walletId != null ? walletId.trim() : "";
        String normalizedCurrency = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "";
        String keyPrefix = maskKey(idempotencyKey);

        if (tenant == null || tenant.isBlank()) {
            OutcomeMapper.MappedOutcome out = outcomeMapper.serviceUnavailable("TENANT_CONTEXT_MISSING", "Tenant context is missing");
            logRequest(tenant, cleanWalletId, null, out.outcomeCode(), 0L, keyPrefix);
            return out.response();
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            OutcomeMapper.MappedOutcome out = outcomeMapper.serviceUnavailable("MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
            logRequest(tenant, cleanWalletId, null, out.outcomeCode(), 0L, keyPrefix);
            return out.response();
        }

        String cleanKey = idempotencyKey.trim();
        String hash = computeHash(type, cleanWalletId, amount, normalizedCurrency);

        LockHandle idemLock;
        try {
            Optional<LockHandle> acquired = lockService.tryAcquire(
                    keyBuilder.idempotencyLock(cleanKey), IDEM_LOCK_TTL, IDEM_LOCK_WAIT);
            if (acquired.isEmpty()) {
                OutcomeMapper.MappedOutcome out = outcomeMapper.inProgress(null);
                logRequest(tenant, cleanWalletId, null, out.outcomeCode(), elapsedMs(startNs), keyPrefix);
                return out.response();
            }
            idemLock = acquired.get();
        } catch (RuntimeException e) {
            OutcomeMapper.MappedOutcome out = outcomeMapper.serviceUnavailable("REDIS_UNAVAILABLE", "Idempotency lock store unavailable");
            logRequest(tenant, cleanWalletId, null, out.outcomeCode(), elapsedMs(startNs), keyPrefix);
            return out.response();
        }

        LockHandle walletLock = null;
        LedgerTransaction tx = null;
        long lockWaitMs = 0L;

        try {
            Optional<LedgerTransaction> existingOpt = transactionRepository.findByIdempotencyKey(cleanKey);
            if (existingOpt.isPresent()) {
                LedgerTransaction stored = existingOpt.get();
                if (!hash.equals(stored.getRequestHash())) {
                    OutcomeMapper.MappedOutcome out = outcomeMapper.conflict();
                    logRequest(tenant, cleanWalletId, stored.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                    return out.response();
                }
                if (stored.getStatus() == TransactionStatus.PENDING) {
                    OutcomeMapper.MappedOutcome out = outcomeMapper.inProgress(stored.getTransactionId());
                    logRequest(tenant, cleanWalletId, stored.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                    return out.response();
                }
                OutcomeMapper.MappedOutcome out = replayResponseBuilder.build(stored);
                logRequest(tenant, cleanWalletId, stored.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                return out.response();
            }

            String txId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            LedgerTransaction pending = new LedgerTransaction();
            pending.setTransactionId(txId);
            pending.setIdempotencyKey(cleanKey);
            pending.setRequestHash(hash);
            pending.setType(type);
            pending.setWalletId(cleanWalletId);
            pending.setAmount(amount);
            pending.setCurrency(normalizedCurrency);
            pending.setStatus(TransactionStatus.PENDING);

            tx = stateService.insertPending(pending);

            long waitStart = System.nanoTime();
            try {
                Optional<LockHandle> wLockOpt = lockService.tryAcquire(
                        keyBuilder.walletLock(cleanWalletId), WALLET_LOCK_TTL, WALLET_LOCK_WAIT);
                lockWaitMs = elapsedMs(waitStart);
                if (wLockOpt.isEmpty()) {
                    stateService.discardPending(tx.getTransactionId());
                    OutcomeMapper.MappedOutcome out = outcomeMapper.lockNotAcquired();
                    logRequest(tenant, cleanWalletId, tx.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                    return out.response();
                }
                walletLock = wLockOpt.get();
            } catch (RuntimeException e) {
                lockWaitMs = elapsedMs(waitStart);
                stateService.discardPending(tx.getTransactionId());
                OutcomeMapper.MappedOutcome out = outcomeMapper.serviceUnavailable("REDIS_UNAVAILABLE", "Wallet lock store unavailable");
                logRequest(tenant, cleanWalletId, tx.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                return out.response();
            }

            try {
                PostingResult res = postingService.post(
                        new PostingCommand(tx.getTransactionId(), type, cleanWalletId, amount, normalizedCurrency)
                );
                OutcomeMapper.MappedOutcome out = outcomeMapper.mapPosting(tx, res);
                logRequest(tenant, cleanWalletId, tx.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                return out.response();
            } catch (WalletNotFoundException e) {
                stateService.discardPending(tx.getTransactionId());
                OutcomeMapper.MappedOutcome out = outcomeMapper.walletNotFound(e);
                logRequest(tenant, cleanWalletId, tx.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                return out.response();
            } catch (RuntimeException unexpected) {
                stateService.markRolledBack(tx.getTransactionId(), FailureCode.SYSTEM_ERROR);
                OutcomeMapper.MappedOutcome out = outcomeMapper.rolledBack(tx);
                logRequest(tenant, cleanWalletId, tx.getTransactionId(), out.outcomeCode(), lockWaitMs, keyPrefix);
                return out.response();
            }
        } catch (DataAccessException e) {
            if (tx != null) {
                try {
                    stateService.markRolledBack(tx.getTransactionId(), FailureCode.SYSTEM_ERROR);
                } catch (Exception ignored) {
                }
            }
            OutcomeMapper.MappedOutcome out = outcomeMapper.serviceUnavailable("DATASTORE_UNAVAILABLE", "Backing store unavailable");
            logRequest(tenant, cleanWalletId, tx != null ? tx.getTransactionId() : null, out.outcomeCode(), lockWaitMs, keyPrefix);
            return out.response();
        } finally {
            safeRelease(walletLock, "walletLock");
            safeRelease(idemLock, "idemLock");
        }
    }

    private void safeRelease(LockHandle handle, String lockName) {
        if (handle == null) {
            return;
        }
        try {
            lockService.release(handle);
        } catch (RuntimeException ex) {
            log.warn("Failed to release {} (will expire via TTL): {}", lockName, ex.getMessage());
        }
    }

    private void logRequest(String tenant,
                            String walletId,
                            String transactionId,
                            String outcome,
                            long lockWaitMs,
                            String keyPrefix) {
        log.info("ledger_cmd tenant={} walletId={} transactionId={} outcome={} lockWaitMs={} keyPrefix={}",
                tenant, walletId, transactionId, outcome, lockWaitMs, keyPrefix);
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "none";
        }
        String trimmed = key.trim();
        return trimmed.length() <= 6 ? "***" : trimmed.substring(0, 6) + "...";
    }

    private String computeHash(TransactionType type, String walletId, BigDecimal amount, String currency) {
        String normalizedAmt = amount != null ? amount.stripTrailingZeros().toPlainString() : "0";
        String canonical = (type != null ? type.name() : "") + "|" + walletId + "|" + normalizedAmt + "|" + currency;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}

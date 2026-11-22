package com.ledgerguard.ledger.api;

import com.ledgerguard.ledger.entity.TransactionType;
import com.ledgerguard.lock.LockHandle;
import com.ledgerguard.lock.LockService;
import com.ledgerguard.lock.RedisKeyBuilder;
import com.ledgerguard.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TransactionCommandService {

    public static final Duration TOTAL_BUDGET = Duration.ofSeconds(25);
    public static final Duration BEFORE_WALLET_LOCK_MIN = Duration.ofSeconds(19);
    public static final Duration AFTER_WALLET_LOCK_MIN = Duration.ofSeconds(16);

    private final LockService lockService;
    private final RedisKeyBuilder keyBuilder;
    private final Map<String, StoredOutcome> idempotencyStore = new ConcurrentHashMap<>();

    private record StoredOutcome(String fingerprint, HttpStatus status, Map<String, Object> body) {}

    @Autowired
    public TransactionCommandService(LockService lockService, RedisKeyBuilder keyBuilder) {
        this.lockService = lockService;
        this.keyBuilder = keyBuilder;
    }

    public ResponseEntity<Map<String, Object>> create(
            String idempotencyKey,
            TransactionType type,
            String walletId,
            BigDecimal amount,
            String currency) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        String safeType = type != null ? type.name() : "CHARGE";
        String safeCurrency = currency != null ? currency.trim().toUpperCase() : "INR";
        BigDecimal normalizedAmount = amount != null ? amount.stripTrailingZeros() : BigDecimal.ZERO;
        String fingerprint = safeType + "|" + walletId + "|" + normalizedAmount.toPlainString() + "|" + safeCurrency;
        String storeKey = tenantId + ":" + (idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString());

        StoredOutcome existing = idempotencyStore.get(storeKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                Map<String, Object> conflict = new LinkedHashMap<>();
                conflict.put("success", false);
                conflict.put("code", "IDEMPOTENCY_KEY_REUSED");
                conflict.put("message", "Idempotency key reused with different payload");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .cacheControl(CacheControl.noStore())
                        .body(conflict);
            }
            return ResponseEntity.status(existing.status())
                    .cacheControl(CacheControl.noStore())
                    .header("Idempotent-Replayed", "true")
                    .body(existing.body());
        }

        AtomicReference<ResponseEntity<Map<String, Object>>> outcomeHolder = new AtomicReference<>();
        String finalTenantId = tenantId;
        ResponseEntity<Map<String, Object>> guardResp = executeWithBudgetGuards(
                finalTenantId,
                idempotencyKey,
                walletId,
                () -> {},
                () -> {},
                () -> {
                    String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("transactionId", txnId);
                    data.put("walletId", walletId);
                    data.put("type", safeType);
                    data.put("amount", amount);
                    data.put("currency", safeCurrency);
                    data.put("status", "SUCCESS");

                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("success", true);
                    body.put("code", "POSTED");
                    body.put("tenantId", finalTenantId);
                    body.put("idempotencyKey", idempotencyKey);
                    body.put("data", data);
                    body.put("timestamp", Instant.now().toString());

                    ResponseEntity<Map<String, Object>> created = ResponseEntity.status(HttpStatus.CREATED)
                            .cacheControl(CacheControl.noStore())
                            .header("Idempotent-Replayed", "false")
                            .body(body);
                    outcomeHolder.set(created);
                    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                        idempotencyStore.putIfAbsent(storeKey, new StoredOutcome(fingerprint, HttpStatus.CREATED, body));
                    }
                }
        );

        if (outcomeHolder.get() != null) {
            return outcomeHolder.get();
        }
        return guardResp;
    }

    public ResponseEntity<Map<String, Object>> executeWithBudgetGuards(
            String tenantId,
            String idempotencyKey,
            String walletId,
            Runnable insertPendingAction,
            Runnable discardPendingAction,
            Runnable postLedgerAction
    ) {
        RequestBudget budget = RequestBudget.start(TOTAL_BUDGET);
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        boolean pendingInserted = false;
        Optional<LockHandle> walletLock = Optional.empty();

        try {
            insertPendingAction.run();
            pendingInserted = true;

            budget.requireAtLeast(BEFORE_WALLET_LOCK_MIN);

            String walletLockKey = keyBuilder.walletLock(walletId);
            Duration remainingForLock = budget.remaining();
            Duration maxWait = remainingForLock.compareTo(Duration.ofSeconds(2)) < 0
                    ? remainingForLock
                    : Duration.ofSeconds(2);

            walletLock = lockService.tryAcquire(walletLockKey, Duration.ofSeconds(5), maxWait);
            if (walletLock.isEmpty()) {
                if (pendingInserted) {
                    safeDiscardPending(discardPendingAction);
                }
                return serviceUnavailable("LOCK_CONTENTION", "Unable to acquire wallet lock within budget", traceId, null);
            }

            budget.requireAtLeast(AFTER_WALLET_LOCK_MIN);

            postLedgerAction.run();
            pendingInserted = false;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("code", "POSTED");
            body.put("tenantId", tenantId);
            body.put("idempotencyKey", idempotencyKey);
            body.put("traceId", traceId);
            body.put("timestamp", Instant.now().toString());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (RequestBudget.BudgetExhaustedException ex) {
            releaseIfPresent(walletLock);
            if (pendingInserted) {
                safeDiscardPending(discardPendingAction);
            }
            return serviceUnavailable("REQUEST_TIMEOUT", ex.getMessage(), traceId, "1");

        } catch (CannotCreateTransactionException | DataAccessResourceFailureException | QueryTimeoutException ex) {
            releaseIfPresent(walletLock);
            if (pendingInserted) {
                safeDiscardPending(discardPendingAction);
            }
            return serviceUnavailable("SERVICE_UNAVAILABLE", "Backing datastore or connection pool unavailable", traceId, null);

        } catch (RuntimeException ex) {
            releaseIfPresent(walletLock);
            if (pendingInserted) {
                safeDiscardPending(discardPendingAction);
            }
            throw ex;
        } finally {
            releaseIfPresent(walletLock);
        }
    }

    private void releaseIfPresent(Optional<LockHandle> handleOpt) {
        handleOpt.ifPresent(h -> {
            try {
                lockService.release(h);
            } catch (RuntimeException ignored) {
            }
        });
    }

    private void safeDiscardPending(Runnable discardPendingAction) {
        try {
            discardPendingAction.run();
        } catch (RuntimeException ignored) {
        }
    }

    private ResponseEntity<Map<String, Object>> serviceUnavailable(
            String code,
            String message,
            String traceId,
            String retryAfterSeconds
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("message", message);
        payload.put("code", code);
        payload.put("traceId", traceId);
        payload.put("timestamp", Instant.now().toString());

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE);
        if (retryAfterSeconds != null) {
            builder.header("Retry-After", retryAfterSeconds);
        }
        return builder.body(payload);
    }
}

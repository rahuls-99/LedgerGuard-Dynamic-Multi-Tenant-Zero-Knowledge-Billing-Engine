package com.ledgerguard.ledger.api;

import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.posting.PostingResult;
import com.ledgerguard.ledger.posting.WalletNotFoundException;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OutcomeMapper {

    public record MappedOutcome(
            ResponseEntity<Map<String, Object>> response,
            String outcomeCode,
            String transactionId,
            String status
    ) {}

    public MappedOutcome mapPosting(LedgerTransaction tx, PostingResult res) {
        String txId = tx.getTransactionId();
        if (res.status() == TransactionStatus.SUCCESS) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("transactionId", txId);
            data.put("status", TransactionStatus.SUCCESS.name());
            data.put("failureCode", null);
            data.put("balanceAfter", res.balanceAfter());
            data.put("idempotentReplay", false);

            ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.CREATED)
                    .body(successBody("Ledger transaction posted", "SUCCESS", data));
            return new MappedOutcome(entity, "SUCCESS", txId, TransactionStatus.SUCCESS.name());
        }

        if (res.status() == TransactionStatus.FAILED) {
            String code = res.failureCode() != null ? res.failureCode() : "LEDGER_FAILED";
            Map<String, Object> data = txData(txId, TransactionStatus.FAILED.name());
            ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.UNPROCESSABLE_CONTENT)
                    .body(errorBody(code, "Transaction failed: " + code, data));
            return new MappedOutcome(entity, code, txId, TransactionStatus.FAILED.name());
        }

        Map<String, Object> data = txData(txId, TransactionStatus.ROLLED_BACK.name());
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("TRANSACTION_ROLLED_BACK", "Transaction rolled back", data));
        return new MappedOutcome(entity, "TRANSACTION_ROLLED_BACK", txId, TransactionStatus.ROLLED_BACK.name());
    }

    public MappedOutcome rolledBack(LedgerTransaction tx) {
        String txId = tx.getTransactionId();
        Map<String, Object> data = txData(txId, TransactionStatus.ROLLED_BACK.name());
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("TRANSACTION_ROLLED_BACK", "Transaction rolled back due to system error", data));
        return new MappedOutcome(entity, "TRANSACTION_ROLLED_BACK", txId, TransactionStatus.ROLLED_BACK.name());
    }

    public MappedOutcome replay(LedgerTransaction tx, BigDecimal customerBalanceAfter) {
        String txId = tx.getTransactionId();
        String st = tx.getStatus().name();

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("transactionId", txId);
            data.put("status", st);
            data.put("failureCode", null);
            data.put("balanceAfter", customerBalanceAfter);
            data.put("idempotentReplay", true);

            ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.CREATED)
                    .header("Idempotent-Replayed", "true")
                    .body(successBody("Idempotent replay", "REPLAY_SUCCESS", data));
            return new MappedOutcome(entity, "REPLAY_SUCCESS", txId, st);
        }

        if (tx.getStatus() == TransactionStatus.FAILED) {
            String code = tx.getFailureReason() != null ? tx.getFailureReason() : "LEDGER_FAILED";
            Map<String, Object> data = txData(txId, st);
            ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.UNPROCESSABLE_CONTENT)
                    .header("Idempotent-Replayed", "true")
                    .body(errorBody(code, "Idempotent replay of failed transaction", data));
            return new MappedOutcome(entity, "REPLAY_" + code, txId, st);
        }

        Map<String, Object> data = txData(txId, TransactionStatus.ROLLED_BACK.name());
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Idempotent-Replayed", "true")
                .body(errorBody("TRANSACTION_ROLLED_BACK", "Idempotent replay of rolled back transaction", data));
        return new MappedOutcome(entity, "REPLAY_ROLLED_BACK", txId, TransactionStatus.ROLLED_BACK.name());
    }

    public MappedOutcome inProgress(String existingTxId) {
        Map<String, Object> data = txData(existingTxId, TransactionStatus.PENDING.name());
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.CONFLICT)
                .header(HttpHeaders.RETRY_AFTER, "2")
                .body(errorBody("TRANSACTION_IN_PROGRESS", "Transaction is currently in progress", data));
        return new MappedOutcome(entity, "IN_PROGRESS", existingTxId, TransactionStatus.PENDING.name());
    }

    public MappedOutcome conflict() {
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(errorBody("IDEMPOTENCY_PAYLOAD_MISMATCH", "Idempotency-Key already used with a different payload", null));
        return new MappedOutcome(entity, "IDEMPOTENCY_PAYLOAD_MISMATCH", null, null);
    }

    public MappedOutcome lockNotAcquired() {
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.CONFLICT)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(errorBody("LOCK_NOT_ACQUIRED", "Wallet lock is currently held by another request", null));
        return new MappedOutcome(entity, "LOCK_NOT_ACQUIRED", null, null);
    }

    public MappedOutcome serviceUnavailable(String code, String message) {
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "3")
                .body(errorBody(code, message, null));
        return new MappedOutcome(entity, code, null, null);
    }

    public MappedOutcome walletNotFound(WalletNotFoundException ex) {
        ResponseEntity<Map<String, Object>> entity = baseHeaders(HttpStatus.NOT_FOUND)
                .body(errorBody("WALLET_NOT_FOUND", ex.getMessage(), null));
        return new MappedOutcome(entity, "WALLET_NOT_FOUND", null, null);
    }

    private ResponseEntity.BodyBuilder baseHeaders(HttpStatus status) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore());
    }

    private Map<String, Object> txData(String transactionId, String status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("transactionId", transactionId);
        map.put("status", status);
        return map;
    }

    private Map<String, Object> successBody(String message, String code, Map<String, Object> data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("code", code);
        body.put("traceId", currentTraceId());
        body.put("timestamp", Instant.now().toString());
        body.put("data", data);
        return body;
    }

    private Map<String, Object> errorBody(String code, String message, Map<String, Object> data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("code", code);
        body.put("traceId", currentTraceId());
        body.put("timestamp", Instant.now().toString());
        body.put("data", data);
        return body;
    }

    private String currentTraceId() {
        String trace = MDC.get("traceId");
        return (trace != null && !trace.isBlank()) ? trace : UUID.randomUUID().toString();
    }
}
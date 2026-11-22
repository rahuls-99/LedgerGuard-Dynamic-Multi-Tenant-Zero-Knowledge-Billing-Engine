package com.ledgerguard.ledger.state;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.posting.CurrencyMismatchException;
import com.ledgerguard.ledger.posting.InsufficientFundsException;
import com.ledgerguard.ledger.posting.WalletNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionTimedOutException;
import java.sql.SQLException;
@Component
public class FailureClassifier {
    private static final Logger log = LoggerFactory.getLogger(FailureClassifier.class);
    public record Classification(TransactionStatus status, FailureCode code, boolean businessFailure) {}
    public Classification classify(Throwable ex) {
        String traceId = MDC.get("traceId");
        if (ex instanceof InsufficientFundsException) return new Classification(TransactionStatus.FAILED, FailureCode.INSUFFICIENT_FUNDS, true);
        if (ex instanceof CurrencyMismatchException) return new Classification(TransactionStatus.FAILED, FailureCode.CURRENCY_MISMATCH, true);
        if (ex instanceof WalletNotFoundException) {
            log.warn("Wallet not found traceId={}: {}", traceId, ex.getMessage());
            return new Classification(TransactionStatus.ROLLED_BACK, FailureCode.SYSTEM_ERROR, false);
        }
        if (isDeadlockOrLockWait(ex)) {
            log.warn("Deadlock/lock timeout traceId={}", traceId, ex);
            return new Classification(TransactionStatus.ROLLED_BACK, FailureCode.DEADLOCK_RETRIES_EXHAUSTED, false);
        }
        if (ex instanceof QueryTimeoutException || ex instanceof TransactionTimedOutException) {
            log.error("Transaction timeout traceId={}", traceId, ex);
            return new Classification(TransactionStatus.ROLLED_BACK, FailureCode.TIMEOUT, false);
        }
        if (ex instanceof DataIntegrityViolationException) {
            log.error("DB integrity violation traceId={}", traceId, ex);
            return new Classification(TransactionStatus.ROLLED_BACK, FailureCode.SYSTEM_ERROR, false);
        }
        log.error("Unexpected posting failure traceId={}", traceId, ex);
        return new Classification(TransactionStatus.ROLLED_BACK, FailureCode.SYSTEM_ERROR, false);
    }
    public boolean isDeadlockOrLockWait(Throwable ex) {
        Throwable curr = ex;
        while (curr != null) {
            if (curr instanceof DeadlockLoserDataAccessException || curr instanceof CannotAcquireLockException) return true;
            if (curr instanceof SQLException sqlEx && (sqlEx.getErrorCode() == 1213 || sqlEx.getErrorCode() == 1205)) return true;
            curr = curr.getCause();
        }
        return false;
    }
}

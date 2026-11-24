package com.ledgerguard.ledger.posting;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.entity.TransactionType;
import com.ledgerguard.ledger.entity.Wallet;
import com.ledgerguard.ledger.repository.WalletRepository;
import com.ledgerguard.ledger.state.FailureClassifier;
import com.ledgerguard.ledger.state.TransactionStateService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Locale;
@Service
public class LedgerPostingServiceImpl implements LedgerPostingService {
  private static final int MAX_DEADLOCK_RETRIES = 15;
  private final WalletRepository walletRepository;
  private final PostingTransactionExecutor transactionExecutor;
  private final TransactionStateService stateService;
  private final FailureClassifier failureClassifier;
  public LedgerPostingServiceImpl(WalletRepository walletRepository, PostingTransactionExecutor transactionExecutor, TransactionStateService stateService, FailureClassifier failureClassifier) {
    this.walletRepository = walletRepository;
    this.transactionExecutor = transactionExecutor;
    this.stateService = stateService;
    this.failureClassifier = failureClassifier;
  }
  @Override
  public PostingResult post(PostingCommand cmd) {
    String currency = cmd.currency().toUpperCase(Locale.ROOT);
    String prefix = (cmd.type() == TransactionType.TOPUP) ? "sys-fund-" : "sys-rev-";
    Wallet systemWallet = walletRepository.findSystemWalletByCurrencyAndPrefix(currency, prefix).orElseGet(() -> walletRepository.findFirstBySystemTrueAndCurrency(currency).orElseThrow(() -> new WalletNotFoundException("System wallet missing for " + currency)));
    int attempt = 0;
    while (true) {
      attempt++;
      try {
        BigDecimal balanceAfter = transactionExecutor.executeAtomicPosting(cmd, systemWallet.getId());
        return new PostingResult(TransactionStatus.SUCCESS, null, balanceAfter);
      } catch (Exception ex) {
        if (failureClassifier.isDeadlockOrLockWait(ex) && attempt < MAX_DEADLOCK_RETRIES) {
          try { Thread.sleep(15L * Math.min(attempt, 5)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
          continue;
        }
        var classification = failureClassifier.classify(ex);
        if (classification.businessFailure()) stateService.markFailed(cmd.transactionId(), classification.code());
        else stateService.markRolledBack(cmd.transactionId(), classification.code());
        if (ex instanceof WalletNotFoundException wnf) throw wnf;
        return new PostingResult(classification.status(), classification.code().name(), null);
      }
    }
  }
}
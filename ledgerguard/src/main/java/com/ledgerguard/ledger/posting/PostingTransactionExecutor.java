package com.ledgerguard.ledger.posting;
import com.ledgerguard.ledger.entity.*;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import com.ledgerguard.ledger.repository.WalletLedgerEntryRepository;
import com.ledgerguard.ledger.repository.WalletRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
@Component
public class PostingTransactionExecutor {
    private final WalletRepository walletRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final WalletLedgerEntryRepository entryRepository;
    private final ObjectProvider<FailureInjector> failureInjectorProvider;
    public PostingTransactionExecutor(WalletRepository walletRepository,
                                      LedgerTransactionRepository transactionRepository,
                                      WalletLedgerEntryRepository entryRepository,
                                      ObjectProvider<FailureInjector> failureInjectorProvider) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.failureInjectorProvider = failureInjectorProvider;
    }
    @Transactional(timeout = 5, rollbackFor = Exception.class)
    public BigDecimal executeAtomicPosting(PostingCommand cmd, String systemWalletId) {
        if (cmd.amount() == null || cmd.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        BigDecimal normalizedAmount = cmd.amount().setScale(2, RoundingMode.UNNECESSARY);
        List<String> sortedIds = List.of(cmd.walletId(), systemWalletId).stream().distinct().sorted().toList();
        List<Wallet> lockedWallets = walletRepository.findAllByIdInOrderByIdAscForUpdate(sortedIds);
        Map<String, Wallet> walletMap = lockedWallets.stream().collect(Collectors.toMap(Wallet::getId, Function.identity()));
        Wallet customerWallet = walletMap.get(cmd.walletId());
        if (customerWallet == null) throw new WalletNotFoundException("Customer wallet not found: " + cmd.walletId());
        Wallet systemWallet = walletMap.get(systemWalletId);
        if (systemWallet == null) throw new WalletNotFoundException("System wallet not found: " + systemWalletId);
        if (!customerWallet.getCurrency().equalsIgnoreCase(cmd.currency()) || !systemWallet.getCurrency().equalsIgnoreCase(cmd.currency())) {
            throw new CurrencyMismatchException("Currency mismatch for wallet " + cmd.walletId());
        }
        Wallet debitWallet = (cmd.type() == TransactionType.CHARGE) ? customerWallet : systemWallet;
        Wallet creditWallet = (cmd.type() == TransactionType.CHARGE) ? systemWallet : customerWallet;
        if (!debitWallet.isSystem() && debitWallet.getBalance().compareTo(normalizedAmount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in wallet " + debitWallet.getId());
        }
        BigDecimal newDebitBalance = debitWallet.getBalance().subtract(normalizedAmount);
        BigDecimal newCreditBalance = creditWallet.getBalance().add(normalizedAmount);
        debitWallet.setBalance(newDebitBalance);
        walletRepository.saveAndFlush(debitWallet);
        FailureInjector injector = failureInjectorProvider.getIfAvailable();
        if (injector != null) injector.checkpoint(FailureInjector.Point.AFTER_DEBIT);
        creditWallet.setBalance(newCreditBalance);
        walletRepository.saveAndFlush(creditWallet);
        entryRepository.saveAllAndFlush(List.of(
                new WalletLedgerEntry("le_" + UUID.randomUUID().toString().replace("-", ""), cmd.transactionId(), debitWallet.getId(), EntryDirection.DEBIT, normalizedAmount, newDebitBalance),
                new WalletLedgerEntry("le_" + UUID.randomUUID().toString().replace("-", ""), cmd.transactionId(), creditWallet.getId(), EntryDirection.CREDIT, normalizedAmount, newCreditBalance)
        ));
        if (transactionRepository.transitionFromPending(cmd.transactionId(), TransactionStatus.SUCCESS, null) == 0) {
            throw new IllegalStateException("Transaction is no longer PENDING: " + cmd.transactionId());
        }
        if (injector != null) injector.checkpoint(FailureInjector.Point.BEFORE_COMMIT);
        return customerWallet.getBalance();
    }
}

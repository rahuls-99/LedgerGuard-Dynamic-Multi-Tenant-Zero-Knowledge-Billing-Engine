package com.ledgerguard.ledger.api;
import com.ledgerguard.ledger.api.LedgerApiDtos.*;
import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.entity.Wallet;
import com.ledgerguard.ledger.posting.LedgerPostingService;
import com.ledgerguard.ledger.posting.PostingCommand;
import com.ledgerguard.ledger.posting.PostingResult;
import com.ledgerguard.ledger.posting.WalletNotFoundException;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import com.ledgerguard.ledger.repository.WalletLedgerEntryRepository;
import com.ledgerguard.ledger.repository.WalletRepository;
import com.ledgerguard.ledger.state.TransactionStateService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1")
public class WalletLedgerController {
    private final WalletRepository walletRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final WalletLedgerEntryRepository entryRepository;
    private final TransactionStateService stateService;
    private final LedgerPostingService postingService;
    public WalletLedgerController(WalletRepository walletRepository,
                                  LedgerTransactionRepository transactionRepository,
                                  WalletLedgerEntryRepository entryRepository,
                                  TransactionStateService stateService,
                                  LedgerPostingService postingService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.stateService = stateService;
        this.postingService = postingService;
    }
    @PostMapping("/wallets")
    public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest req) {
        if (walletRepository.existsById(req.walletId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "WALLET_ALREADY_EXISTS", "walletId", req.walletId()));
        }
        BigDecimal initial = req.initialBalance() != null ? req.initialBalance() : BigDecimal.ZERO;
        Wallet wallet = new Wallet(
                req.walletId().trim(),
                req.ownerId().trim(),
                req.currency().trim().toUpperCase(Locale.ROOT),
                initial,
                false
        );
        Wallet saved = walletRepository.saveAndFlush(wallet);
        return ResponseEntity.status(HttpStatus.CREATED).body(toWalletResponse(saved));
    }
    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<?> getWalletStatement(@PathVariable String walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
        var entries = entryRepository.findTop50ByWalletIdOrderByCreatedAtDesc(walletId).stream()
                .map(e -> new LedgerEntryView(
                        e.getEntryId(),
                        e.getTransactionId(),
                        e.getDirection().name(),
                        e.getAmount(),
                        e.getBalanceAfter(),
                        e.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(new WalletStatementResponse(toWalletResponse(wallet), entries));
    }
    @PostMapping("/ledger/post")
    public ResponseEntity<?> postTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PostLedgerRequest req) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "MISSING_IDEMPOTENCY_KEY", "message", "Idempotency-Key header is required"));
        }
        String cleanKey = idempotencyKey.trim();
        String normalizedCurrency = req.currency().trim().toUpperCase(Locale.ROOT);
        String canonicalPayload = req.type().name() + "|" + req.walletId().trim() + "|" + req.amount().stripTrailingZeros().toPlainString() + "|" + normalizedCurrency;
        String requestHash = sha256(canonicalPayload);
        Optional<LedgerTransaction> existingOpt = transactionRepository.findByIdempotencyKey(cleanKey);
        if (existingOpt.isPresent()) {
            return buildReplayResponse(existingOpt.get(), requestHash);
        }
        String txId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        LedgerTransaction pendingTx = new LedgerTransaction();
        pendingTx.setTransactionId(txId);
        pendingTx.setIdempotencyKey(cleanKey);
        pendingTx.setRequestHash(requestHash);
        pendingTx.setType(req.type());
        pendingTx.setWalletId(req.walletId().trim());
        pendingTx.setAmount(req.amount());
        pendingTx.setCurrency(normalizedCurrency);
        pendingTx.setStatus(TransactionStatus.PENDING);
        try {
            stateService.insertPending(pendingTx);
        } catch (DataIntegrityViolationException dup) {
            LedgerTransaction concurrent = transactionRepository.findByIdempotencyKey(cleanKey)
                    .orElseThrow(() -> dup);
            return buildReplayResponse(concurrent, requestHash);
        }
        PostingResult result = postingService.post(
                new PostingCommand(txId, req.type(), req.walletId().trim(), req.amount(), normalizedCurrency)
        );
        HttpStatus status = (result.status() == TransactionStatus.SUCCESS)
                ? HttpStatus.OK
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(
                new PostLedgerResponse(txId, result.status(), result.failureCode(), result.balanceAfter(), false)
        );
    }
    private ResponseEntity<?> buildReplayResponse(LedgerTransaction existing, String incomingHash) {
        if (!existing.getRequestHash().equals(incomingHash)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(
                            "error", "IDEMPOTENCY_PAYLOAD_MISMATCH",
                            "message", "Idempotency-Key already used with a different request payload"
                    ));
        }
        if (existing.getStatus() == TransactionStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "TRANSACTION_IN_PROGRESS",
                            "transactionId", existing.getTransactionId()
                    ));
        }
        BigDecimal currentBalance = walletRepository.findById(existing.getWalletId())
                .map(Wallet::getBalance)
                .orElse(null);
        HttpStatus status = (existing.getStatus() == TransactionStatus.SUCCESS)
                ? HttpStatus.OK
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status)
                .header("X-Idempotent-Replay", "true")
                .body(new PostLedgerResponse(
                        existing.getTransactionId(),
                        existing.getStatus(),
                        existing.getFailureReason(),
                        currentBalance,
                        true
                ));
    }
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<?> handleWalletNotFound(WalletNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "WALLET_NOT_FOUND", "message", ex.getMessage()));
    }
    private WalletResponse toWalletResponse(Wallet w) {
        return new WalletResponse(w.getId(), w.getOwnerId(), w.getCurrency(), w.getBalance(), w.isSystem(), w.getUpdatedAt());
    }
    private String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
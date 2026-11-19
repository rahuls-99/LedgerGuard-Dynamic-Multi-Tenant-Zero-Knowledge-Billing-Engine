package com.ledgerguard.controller;

import com.ledgerguard.dto.TransactionRequest;
import com.ledgerguard.dto.TransactionResponse;
import com.ledgerguard.exception.InvalidIdempotencyKeyException;
import com.ledgerguard.service.LedgerTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
public class LedgerTransactionController {

    private final LedgerTransactionService transactionService;

    public LedgerTransactionController(LedgerTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Tenant-ID", defaultValue = "tenant-101") String tenantId,
            @Valid @RequestBody TransactionRequest request) throws Exception {

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty() || idempotencyKey.length() > 128) {
            throw new InvalidIdempotencyKeyException(
                    "Valid 'Idempotency-Key' header (1-128 chars) is required.");
        }

        TransactionResponse response = transactionService.processTransaction(
                tenantId.trim(), idempotencyKey.trim(), request);

        HttpStatus status = response.isReplayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
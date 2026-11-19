package com.ledgerguard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerguard.dto.TransactionRequest;
import com.ledgerguard.dto.TransactionResponse;
import com.ledgerguard.entity.IdempotencyRecord;
import com.ledgerguard.entity.LedgerTransaction;
import com.ledgerguard.exception.DuplicateRequestInProgressException;
import com.ledgerguard.repository.IdempotencyRecordRepository;
import com.ledgerguard.repository.LedgerTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerTransactionService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final RedisDistributedLockService lockService;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public LedgerTransactionService(
            RedisDistributedLockService lockService,
            IdempotencyRecordRepository idempotencyRepository,
            LedgerTransactionRepository transactionRepository,
            ObjectMapper objectMapper) {
        this.lockService = lockService;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    public TransactionResponse processTransaction(
            String orgId, String idempotencyKey, TransactionRequest request) throws Exception {

        String lockKey = "lock:idempotency:" + orgId + ":" + idempotencyKey;
        String lockToken = lockService.acquireLock(lockKey, LOCK_TTL);

        // 1. Concurrent duplicate request check (Redis Lock)
        if (lockToken == null) {
            throw new DuplicateRequestInProgressException(
                    "Request with Idempotency-Key '" + idempotencyKey + "' is currently being processed.");
        }

        try {
            // 2. DB mein check karein ki kya yeh key pehle process ho chuki hai
            Optional<IdempotencyRecord> existingRecord =
                    idempotencyRepository.findByOrganizationIdAndIdempotencyKey(orgId, idempotencyKey);

            if (existingRecord.isPresent()) {
                // Purana response reuse karein
                TransactionResponse cached = objectMapper.readValue(
                        existingRecord.get().getResponsePayload(), TransactionResponse.class);
                cached.setReplayed(true);
                return cached;
            }

            // 3. Nayi request hai -> @Transactional block mein save karein
            return executeAndPersistTransaction(orgId, idempotencyKey, request);

        } finally {
            // 4. Hamesha lock release karein
            lockService.releaseLock(lockKey, lockToken);
        }
    }

    @Transactional
    protected TransactionResponse executeAndPersistTransaction(
            String orgId, String idempotencyKey, TransactionRequest request) throws Exception {

        // Unique Transaction ID generate karein
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String transactionId = "TXN-" + datePart + "-" + randomPart;

        // Ledger Transaction save karein
        LedgerTransaction txn = new LedgerTransaction();
        txn.setTransactionId(transactionId);
        txn.setOrganizationId(orgId);
        txn.setAmount(request.getAmount());
        txn.setCurrency(request.getCurrency());
        txn.setReferenceId(request.getReferenceId());
        txn.setStatus("SUCCESS");
        txn.setCreatedAt(Instant.now());
        transactionRepository.save(txn);

        // Response DTO banayein
        TransactionResponse response = new TransactionResponse(
                transactionId, orgId, request.getAmount(), request.getCurrency(), "SUCCESS", false);

        // Idempotency Record save karein (future duplicate requests ke liye)
        IdempotencyRecord record = new IdempotencyRecord();
        record.setOrganizationId(orgId);
        record.setIdempotencyKey(idempotencyKey);
        record.setTransactionId(transactionId);
        record.setResponsePayload(objectMapper.writeValueAsString(response));
        record.setHttpStatus(201);
        idempotencyRepository.save(record);

        return response;
    }
}
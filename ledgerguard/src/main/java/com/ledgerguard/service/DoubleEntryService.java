package com.ledgerguard.service;

import com.ledgerguard.dto.DoubleEntryRequest;
import com.ledgerguard.dto.JournalLineRequest;
import com.ledgerguard.dto.PostedLineResponse;
import com.ledgerguard.entity.JournalEntry;
import com.ledgerguard.entity.LedgerAccount;
import com.ledgerguard.repository.JournalEntryRepository;
import com.ledgerguard.repository.LedgerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DoubleEntryService {

    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;

    public DoubleEntryService(LedgerAccountRepository accountRepository,
                              JournalEntryRepository journalRepository) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Map<String, Object> postDoubleEntry(DoubleEntryRequest request) {
        if (request.getEntries() == null || request.getEntries().size() < 2) {
            throw new IllegalArgumentException("At least 2 journal entries (DEBIT and CREDIT) are required");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalLineRequest line : request.getEntries()) {
            if (line.getAccountId() == null || line.getAccountId().isBlank()) {
                throw new IllegalArgumentException("Account ID is required on every entry");
            }
            if (line.getAmount() == null || line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }
            if ("DEBIT".equalsIgnoreCase(line.getDirection())) {
                totalDebit = totalDebit.add(line.getAmount());
            } else if ("CREDIT".equalsIgnoreCase(line.getDirection())) {
                totalCredit = totalCredit.add(line.getAmount());
            } else {
                throw new IllegalArgumentException("Direction must be DEBIT or CREDIT");
            }
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    "Unbalanced double-entry transaction: totalDebit=" + totalDebit + " != totalCredit=" + totalCredit
            );
        }

        List<JournalLineRequest> sortedLines = new ArrayList<>(request.getEntries());
        sortedLines.sort(Comparator.comparing(JournalLineRequest::getAccountId));

        String journalId = "jrn_" + UUID.randomUUID();
        String currency = (request.getCurrency() == null || request.getCurrency().isBlank())
                ? "INR"
                : request.getCurrency().toUpperCase();
        Instant now = Instant.now();

        List<PostedLineResponse> postedLines = new ArrayList<>();

        for (JournalLineRequest line : sortedLines) {
            LedgerAccount account = accountRepository.findByIdForUpdate(line.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + line.getAccountId()));

            BigDecimal delta = "DEBIT".equalsIgnoreCase(line.getDirection())
                    ? line.getAmount()
                    : line.getAmount().negate();

            BigDecimal newBalance = account.getBalance().add(delta);
            if ("ASSET".equalsIgnoreCase(account.getAccountType()) && newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Insufficient balance in account: " + account.getId());
            }

            account.setBalance(newBalance);
            account.setUpdatedAt(now);
            accountRepository.save(account);

            JournalEntry entry = new JournalEntry();
            entry.setId("je_" + UUID.randomUUID());
            entry.setJournalId(journalId);
            entry.setAccountId(account.getId());
            entry.setDirection(line.getDirection().toUpperCase());
            entry.setAmount(line.getAmount());
            entry.setCurrency(currency);
            entry.setReferenceId(request.getReferenceId());
            entry.setDescription(request.getDescription());
            entry.setCreatedAt(now);
            journalRepository.save(entry);

            postedLines.add(new PostedLineResponse(
                    entry.getId(),
                    account.getId(),
                    entry.getDirection(),
                    entry.getAmount(),
                    newBalance
            ));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("journalId", journalId);
        response.put("referenceId", request.getReferenceId());
        response.put("currency", currency);
        response.put("totalDebit", totalDebit);
        response.put("totalCredit", totalCredit);
        response.put("entries", postedLines);
        response.put("postedAt", now.toString());
        return response;
    }

    @Transactional(readOnly = true)
    public List<LedgerAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntries(String journalId) {
        return journalRepository.findByJournalId(journalId);
    }
}
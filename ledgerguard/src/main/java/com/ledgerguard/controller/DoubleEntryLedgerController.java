package com.ledgerguard.controller;

import com.ledgerguard.dto.DoubleEntryRequest;
import com.ledgerguard.entity.JournalEntry;
import com.ledgerguard.entity.LedgerAccount;
import com.ledgerguard.service.DoubleEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/ledger")
public class DoubleEntryLedgerController {

    private final DoubleEntryService doubleEntryService;

    public DoubleEntryLedgerController(DoubleEntryService doubleEntryService) {
        this.doubleEntryService = doubleEntryService;
    }

    @PostMapping("/journal-entries")
    public ResponseEntity<?> postJournalEntry(@RequestBody DoubleEntryRequest request) {
        try {
            Map<String, Object> result = doubleEntryService.postDoubleEntry(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<LedgerAccount>> listAccounts() {
        return ResponseEntity.ok(doubleEntryService.getAllAccounts());
    }

    @GetMapping("/journal-entries/{journalId}")
    public ResponseEntity<List<JournalEntry>> getJournal(@PathVariable String journalId) {
        return ResponseEntity.ok(doubleEntryService.getJournalEntries(journalId));
    }
}
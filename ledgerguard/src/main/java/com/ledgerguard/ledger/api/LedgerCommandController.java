package com.ledgerguard.ledger.api;

import com.ledgerguard.ledger.entity.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
public class LedgerCommandController {

    public record ChargeRequest(
            @NotBlank String walletId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency
    ) {}

    private final TransactionCommandService commandService;

    public LedgerCommandController(TransactionCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/charge")
    public ResponseEntity<Map<String, Object>> charge(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ChargeRequest req) {
        return commandService.create(
                idempotencyKey,
                TransactionType.CHARGE,
                req.walletId(),
                req.amount(),
                req.currency()
        );
    }
}

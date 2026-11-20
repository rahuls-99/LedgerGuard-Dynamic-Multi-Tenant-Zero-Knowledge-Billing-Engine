package com.ledgerguard.ledger;
import com.ledgerguard.ledger.entity.LedgerTransaction;
import com.ledgerguard.ledger.entity.TransactionStatus;
import com.ledgerguard.ledger.entity.TransactionType;
import com.ledgerguard.ledger.recovery.PendingTransactionRecoveryJob;
import com.ledgerguard.ledger.repository.LedgerTransactionRepository;
import com.ledgerguard.ledger.repository.WalletLedgerEntryRepository;
import com.ledgerguard.ledger.repository.WalletRepository;
import com.ledgerguard.ledger.state.TransactionStateService;
import com.ledgerguard.multitenancy.TenantContext;
import com.ledgerguard.multitenancy.TenantFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "tester", roles = {"ADMIN"})
class LedgerApiAndRecoveryTest {
    @Autowired private WebApplicationContext context;
    @Autowired private TenantFilter tenantFilter;
    @Autowired private WalletRepository walletRepository;
    @Autowired private LedgerTransactionRepository transactionRepository;
    @Autowired private WalletLedgerEntryRepository entryRepository;
    @Autowired private TransactionStateService stateService;
    @Autowired private PendingTransactionRecoveryJob recoveryJob;
    @Autowired private LedgerInvariants invariants;
    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(tenantFilter)
                .apply(springSecurity())
                .build();
    }
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        invariants.assertAll("alpha");
    }
    @Test
    void postCharge_withIdempotencyKey_replaysSafelyAndRejectsPayloadMismatch() throws Exception {
        String walletId = "w-api-" + UUID.randomUUID().toString().substring(0, 8);
        String idemKey = "idem-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletId\":\"" + walletId + "\",\"ownerId\":\"user-1\",\"currency\":\"INR\",\"initialBalance\":500.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(500.00));
        String chargeJson = "{\"type\":\"CHARGE\",\"walletId\":\"" + walletId + "\",\"amount\":120.00,\"currency\":\"INR\"}";
        mockMvc.perform(post("/api/v1/ledger/post")
                        .header("X-Tenant-ID", "alpha")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chargeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.balanceAfter").value(380.00))
                .andExpect(jsonPath("$.idempotentReplay").value(false));
        mockMvc.perform(post("/api/v1/ledger/post")
                        .header("X-Tenant-ID", "alpha")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chargeJson))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.balanceAfter").value(380.00))
                .andExpect(jsonPath("$.idempotentReplay").value(true));
        String tamperedJson = "{\"type\":\"CHARGE\",\"walletId\":\"" + walletId + "\",\"amount\":250.00,\"currency\":\"INR\"}";
        mockMvc.perform(post("/api/v1/ledger/post")
                        .header("X-Tenant-ID", "alpha")
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("IDEMPOTENCY_PAYLOAD_MISMATCH"));
        mockMvc.perform(get("/api/v1/wallets/" + walletId)
                        .header("X-Tenant-ID", "alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.balance").value(380.00))
                .andExpect(jsonPath("$.entries.length()").value(1));
    }
    @Test
    void recoveryJob_marksStuckPendingTransactionRolledBack() {
        TenantContext.setTenantId("alpha");
        String txId = "TXN-STUCK-" + UUID.randomUUID().toString().substring(0, 8);
        LedgerTransaction stuckTx = new LedgerTransaction();
        stuckTx.setTransactionId(txId);
        stuckTx.setRequestHash("hash-stuck");
        stuckTx.setType(TransactionType.CHARGE);
        stuckTx.setWalletId("w-state-test");
        stuckTx.setAmount(new BigDecimal("10.00"));
        stuckTx.setCurrency("INR");
        stuckTx.setStatus(TransactionStatus.PENDING);
        stateService.insertPending(stuckTx);
        int recovered = recoveryJob.recoverTenant("alpha", Duration.ZERO);
        assertTrue(recovered >= 1);
        LedgerTransaction updated = transactionRepository.findByTransactionId(txId).orElseThrow();
        assertEquals(TransactionStatus.ROLLED_BACK, updated.getStatus());
        assertEquals("STUCK_PENDING_RECOVERED", updated.getFailureReason());
        assertTrue(entryRepository.findByTransactionId(txId).isEmpty());
    }
}
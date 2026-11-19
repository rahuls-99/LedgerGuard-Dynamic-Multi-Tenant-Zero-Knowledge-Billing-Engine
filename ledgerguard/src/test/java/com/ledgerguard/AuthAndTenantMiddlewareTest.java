package com.ledgerguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthAndTenantMiddlewareTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String orgAToken;

    @BeforeAll
    void loginAndFetchJwtToken() throws Exception {
        String loginBody = """
                {
                  "orgId": "orgA",
                  "email": "admin@orga.com",
                  "password": "123456"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/real-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        orgAToken = json.get("token").asText();
    }

    @Test
    @DisplayName("AUTH: Should return 401 Unauthorized on wrong password")
    void shouldRejectInvalidLoginCredentials() throws Exception {
        String badLogin = """
                {
                  "orgId": "orgA",
                  "email": "admin@orga.com",
                  "password": "wrong_password"
                }
                """;

        mockMvc.perform(post("/api/real-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLogin))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("MIDDLEWARE: Should block request without JWT token with 401")
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("TENANT: Should return 200 OK with only 4 orgA invoices")
    void shouldReturnOnlyOrgAInvoices() throws Exception {
        mockMvc.perform(get("/api/invoices")
                        .header("Authorization", "Bearer " + orgAToken)
                        .header("X-Org-Id", "orgA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].orgId").value("orgA"));
    }

    @Test
    @DisplayName("STATS: Should return accurate dashboard totals for orgA")
    void shouldReturnAccurateDashboardStatsForOrgA() throws Exception {
        mockMvc.perform(get("/api/invoices/stats")
                        .header("Authorization", "Bearer " + orgAToken)
                        .header("X-Org-Id", "orgA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgId").value("orgA"))
                .andExpect(jsonPath("$.totalRevenue").value(40000.00))
                .andExpect(jsonPath("$.totalInvoices").value(4))
                .andExpect(jsonPath("$.pendingInvoices").value(1))
                .andExpect(jsonPath("$.overdueInvoices").value(1));
    }

    @Test
    @DisplayName("SECURITY: Should block header spoofing with 403 Forbidden")
    void shouldBlockHeaderSpoofingWith403() throws Exception {
        mockMvc.perform(get("/api/invoices")
                        .header("Authorization", "Bearer " + orgAToken)
                        .header("X-Org-Id", "orgB"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("SECURITY: Should block cross-tenant access to invoice 5 with 403")
    void shouldBlockCrossTenantAccessWith403() throws Exception {
        mockMvc.perform(get("/api/invoices/5")
                        .header("Authorization", "Bearer " + orgAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
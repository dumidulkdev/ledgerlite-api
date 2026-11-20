package com.ledgerlite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.dto.CreateWalletRequest;
import com.ledgerlite.dto.MoneyMovementRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static com.ledgerlite.config.OpenApiConfig.IDEMPOTENCY_KEY_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatementControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test void buildsStatementForDateRange() throws Exception {
        UUID walletId = createWallet();
        deposit(walletId, "120.00", "opening");
        withdraw(walletId, "20.00", "cash");

        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2030-01-01T00:00:00Z");

        mockMvc.perform(get("/api/wallets/{id}/statement", walletId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(0))
                .andExpect(jsonPath("$.closingBalance").value(100.00))
                .andExpect(jsonPath("$.totalCredits").value(120.00))
                .andExpect(jsonPath("$.totalDebits").value(20.00))
                .andExpect(jsonPath("$.entries.content.length()").value(greaterThanOrEqualTo(2)));
    }

  @Test void returnsNotFoundForUnknownWallet() throws Exception {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2030-01-01T00:00:00Z");
        mockMvc.perform(get("/api/wallets/{id}/statement", UUID.randomUUID())
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isNotFound());
    }

    private UUID createWallet() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateWalletRequest("Stmt Owner", "USD"));
        String response = mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void deposit(UUID walletId, String amount, String reference) throws Exception {
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal(amount), reference));
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }

    private void withdraw(UUID walletId, String amount, String reference) throws Exception {
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal(amount), reference));
        mockMvc.perform(post("/api/wallets/{id}/withdrawals", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }
}

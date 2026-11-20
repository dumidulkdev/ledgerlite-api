package com.ledgerlite.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.UUID;

import static com.ledgerlite.config.OpenApiConfig.IDEMPOTENCY_KEY_HEADER;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletLedgerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test void depositsIncreaseBalance() throws Exception {
        UUID walletId = createWallet();
        deposit(walletId, "50.00", "opening");
        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test void withdrawsDecreaseBalance() throws Exception {
        UUID walletId = createWallet();
        deposit(walletId, "80.00", "fund");
        withdraw(walletId, "30.00", "cash");
        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test void rejectsOverdraftWithdrawal() throws Exception {
        UUID walletId = createWallet();
        deposit(walletId, "10.00", "seed");
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal("99.00"), "rent"));
        mockMvc.perform(post("/api/wallets/{id}/withdrawals", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test void rejectsNonPositiveAmount() throws Exception {
        UUID walletId = createWallet();
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(BigDecimal.ZERO, "bad"));
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectsMoreThanTwoDecimalPlaces() throws Exception {
        UUID walletId = createWallet();
        String payload = "{\"amount\":12.345,\"reference\":\"precise\"}";
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must have at most 2 decimal places"));
    }

    @Test void exposesLedgerHistory() throws Exception {
        UUID walletId = createWallet();
        deposit(walletId, "15.00", "one");
        deposit(walletId, "10.00", "two");
        mockMvc.perform(get("/api/wallets/{id}/ledger", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)));
    }

    private UUID createWallet() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateWalletRequest("Ledger Owner", "USD"));
        String response = mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return UUID.fromString(node.get("id").asText());
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

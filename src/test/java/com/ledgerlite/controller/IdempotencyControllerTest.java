package com.ledgerlite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.dto.CreateTransferRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test void replaysIdempotentDeposit() throws Exception {
        UUID walletId = createWallet();
        String key = "dep-" + UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal("40.00"), "bonus"));

        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(40.00));

        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(40.00));
    }

    @Test void rejectsConflictingIdempotentDepositBody() throws Exception {
        UUID walletId = createWallet();
        String key = "dep-" + UUID.randomUUID();
        String first = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal("10.00"), "a"));
        String second = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal("20.00"), "b"));

        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isConflict());
    }

    @Test void replaysIdempotentTransfer() throws Exception {
        UUID source = createWallet("S", "USD");
        UUID target = createWallet("T", "USD");
        deposit(source, "100.00");
        CreateTransferRequest request = new CreateTransferRequest(source, target, new BigDecimal("15.00"), "split");
        String key = "xfer-" + UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(request);

        String first = mockMvc.perform(post("/api/transfers")
                        .header(IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/transfers")
                        .header(IDEMPOTENCY_KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(objectMapper.readTree(first).get("id").asText()));
    }

    private UUID createWallet() throws Exception {
        return createWallet("Owner", "USD");
    }

    private UUID createWallet(String owner, String currency) throws Exception {
        String json = objectMapper.writeValueAsString(new CreateWalletRequest(owner, currency));
        String response = mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void deposit(UUID walletId, String amount) throws Exception {
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal(amount), "seed"));
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }
}

package com.ledgerlite.controller;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test void transfersFundsBetweenWallets() throws Exception {
        UUID source = createWallet("Source", "USD");
        UUID target = createWallet("Target", "USD");
        deposit(source, "200.00");

        CreateTransferRequest request = new CreateTransferRequest(source, target, new BigDecimal("75.50"), "utilities");
        String body = mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(75.50))
                .andReturn().getResponse().getContentAsString();
        UUID transferId = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(get("/api/transfers/{id}", transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceWalletId").value(source.toString()));

        mockMvc.perform(get("/api/wallets/{id}", source))
                .andExpect(jsonPath("$.balance").value(124.50));
        mockMvc.perform(get("/api/wallets/{id}", target))
                .andExpect(jsonPath("$.balance").value(75.50));
    }


    @Test void rejectsCurrencyMismatch() throws Exception {
        UUID usd = createWallet("USD", "USD");
        UUID eur = createWallet("EUR", "EUR");
        deposit(usd, "100.00");
        CreateTransferRequest request = new CreateTransferRequest(usd, eur, new BigDecimal("5.00"), "bad");
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Currency mismatch between wallets"));
    }

    @Test void returnsNotFoundForMissingTransfer() throws Exception {
        mockMvc.perform(get("/api/transfers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transfer not found"));
    }

    private UUID createWallet(String owner, String currency) throws Exception {
        String payload = objectMapper.writeValueAsString(new CreateWalletRequest(owner, currency));
        String response = mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void deposit(UUID walletId, String amount) throws Exception {
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal(amount), "fund"));
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }
}
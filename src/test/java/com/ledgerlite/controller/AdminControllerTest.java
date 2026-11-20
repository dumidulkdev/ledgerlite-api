package com.ledgerlite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.domain.Wallet;
import com.ledgerlite.dto.CreateWalletRequest;
import com.ledgerlite.dto.MoneyMovementRequest;
import com.ledgerlite.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.ledgerlite.config.OpenApiConfig.IDEMPOTENCY_KEY_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WalletRepository walletRepository;

    @Test void reportsHealthyLedgerAfterNormalActivity() throws Exception {
        UUID walletId = createWallet();
        deposit(walletId, "45.00");
        mockMvc.perform(get("/api/admin/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.walletMismatches").isEmpty())
                .andExpect(jsonPath("$.transactionImbalances").isEmpty());
    }

    @Test void detectsTamperedWalletBalance() throws Exception {
        UUID walletId = createWallet();
        deposit(walletId, "30.00");
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        wallet.setBalance(new BigDecimal("999.00"));
        walletRepository.save(wallet);

        mockMvc.perform(get("/api/admin/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(false))
                .andExpect(jsonPath("$.walletMismatches[0].walletId").value(walletId.toString()));
    }

    private UUID createWallet() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateWalletRequest("Admin Owner", "USD"));
        String response = mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void deposit(UUID walletId, String amount) throws Exception {
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(new BigDecimal(amount), "fund"));
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }
}

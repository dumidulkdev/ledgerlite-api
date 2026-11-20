package com.ledgerlite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.dto.CreateWalletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletCrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsWallet() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateWalletRequest("Alex Kim", "USD"));
        mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Alex Kim"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void rejectsInvalidCreatePayload() throws Exception {
        mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void rejectsInvalidCurrencyCode() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateWalletRequest("Alex Kim", "US"));
        mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("currency"));
    }

    @Test
    void returnsNotFoundForMissingWallet() throws Exception {
        mockMvc.perform(get("/api/wallets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Wallet not found"));
    }

    @Test
    void listsWalletsWithPagination() throws Exception {
        for (int i = 0; i < 3; i++) {
            String json = objectMapper.writeValueAsString(new CreateWalletRequest("Owner " + i, "USD"));
            mockMvc.perform(post("/api/wallets").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/wallets").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)));
    }
}

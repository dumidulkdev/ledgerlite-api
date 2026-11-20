package com.ledgerlite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.dto.CreateTransferRequest;
import com.ledgerlite.dto.CreateWalletRequest;
import com.ledgerlite.dto.MoneyMovementRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static com.ledgerlite.config.OpenApiConfig.IDEMPOTENCY_KEY_HEADER;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test void persistsNotificationsAfterTransferCommits() throws Exception {
        UUID source = createWallet("Notify Source", "USD");
        UUID target = createWallet("Notify Target", "USD");
        deposit(source, "50.00");

        CreateTransferRequest transfer = new CreateTransferRequest(source, target, new BigDecimal("12.00"), "notify");
        mockMvc.perform(post("/api/transfers")
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/notifications"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content[0].eventType").value("TransferCompleted"))
        );
    }

    @Test void listsNotificationsWithPagination() throws Exception {
        UUID source = createWallet("Pager Source", "EUR");
        UUID target = createWallet("Pager Target", "EUR");
        deposit(source, "40.00");

        for (int i = 0; i < 3; i++) {
            CreateTransferRequest transfer = new CreateTransferRequest(source, target, new BigDecimal("1.00"), "p" + i);
            mockMvc.perform(post("/api/transfers")
                            .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transfer)))
                    .andExpect(status().isOk());
        }

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/notifications").param("size", "2"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(2))
                        .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)))
        );
    }

    private UUID createWallet(String owner, String currency) throws Exception {
        String json = objectMapper.writeValueAsString(new CreateWalletRequest(owner, currency));
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

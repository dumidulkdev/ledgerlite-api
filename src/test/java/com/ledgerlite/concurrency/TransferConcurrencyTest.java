package com.ledgerlite.concurrency;

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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.ledgerlite.config.OpenApiConfig.IDEMPOTENCY_KEY_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferConcurrencyTest {

    private static final int THREADS = 16;
    private static final int TRANSFERS_PER_THREAD = 8;
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("1.00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void conservesMoneyUnderParallelTransfers() throws Exception {
        UUID source = createWallet("Source", "EUR");
        UUID target = createWallet("Target", "EUR");

        BigDecimal initialFunding = TRANSFER_AMOUNT
                .multiply(BigDecimal.valueOf((long) THREADS * TRANSFERS_PER_THREAD));
        deposit(source, initialFunding, "bulk fund");

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> {
                int successes = 0;
                for (int j = 0; j < TRANSFERS_PER_THREAD; j++) {
                    try {
                        CreateTransferRequest request = new CreateTransferRequest(
                                source, target, TRANSFER_AMOUNT, "parallel");
                        MvcResult result = mockMvc.perform(post("/api/transfers")
                                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                .andReturn();
                        if (result.getResponse().getStatus() == 200) {
                            successes++;
                        }
                    } catch (Exception ignored) {
                        // counted as failed attempt
                    }
                }
                return successes;
            });
        }

        List<Future<Integer>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int completed = 0;
        for (Future<Integer> future : futures) {
            completed += future.get();
        }

        MvcResult sourceBalance = mockMvc.perform(get("/api/wallets/{id}", source))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult targetBalance = mockMvc.perform(get("/api/wallets/{id}", target))
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal sourceAmount = new BigDecimal(objectMapper.readTree(sourceBalance.getResponse().getContentAsString())
                .get("balance").asText());
        BigDecimal targetAmount = new BigDecimal(objectMapper.readTree(targetBalance.getResponse().getContentAsString())
                .get("balance").asText());

        assertThat(sourceAmount.signum()).isGreaterThanOrEqualTo(0);
        assertThat(targetAmount.signum()).isGreaterThanOrEqualTo(0);
        assertThat(sourceAmount.add(targetAmount)).isEqualByComparingTo(initialFunding);
        assertThat(completed).isGreaterThan(0);

    }

    private UUID createWallet(String owner, String currency) throws Exception {
        String payload = objectMapper.writeValueAsString(new CreateWalletRequest(owner, currency));
        String response = mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void deposit(UUID walletId, BigDecimal amount, String reference) throws Exception {
        String payload = objectMapper.writeValueAsString(new MoneyMovementRequest(amount, reference));
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
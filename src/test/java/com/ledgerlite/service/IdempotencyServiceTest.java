package com.ledgerlite.service;

import com.ledgerlite.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Test
    void storesAndReplaysCompletedResponses() {
        var record = idempotencyService.reserve("key-1", "/api/transfers", "hash-a");
        idempotencyService.complete(record, 200, "{\"id\":\"123\"}");

        var replay = idempotencyService.findCompleted("key-1", "hash-a");
        assertTrue(replay.isPresent());
        assertEquals(200, replay.get().status());
        assertEquals("{\"id\":\"123\"}", replay.get().body());
    }

    @Test
    void rejectsMismatchedBodyHash() {
        idempotencyService.reserve("key-2", "/api/transfers", "hash-a");
        assertThrows(ConflictException.class, () -> idempotencyService.reserve("key-2", "/api/transfers", "hash-b"));
    }

    @Test
    void hashesIdenticalRequestBodiesConsistently() {
        Map<String, String> body = Map.of("amount", "10.00", "reference", "coffee");
        String first = idempotencyService.hashRequest(body);
        String second = idempotencyService.hashRequest(body);
        assertEquals(first, second);
    }
}
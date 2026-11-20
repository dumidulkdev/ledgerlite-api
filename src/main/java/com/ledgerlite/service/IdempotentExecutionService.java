package com.ledgerlite.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.domain.IdempotencyRecord;
import com.ledgerlite.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotentExecutionService {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotentExecutionService(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> ResponseEntity<T> execute(String idempotencyKey, String endpoint, Object requestBody,
                                           Supplier<T> operation, Class<T> responseType) {
        idempotencyService.requireKey(idempotencyKey);
        String hash = idempotencyService.hashRequest(requestBody);

        Optional<IdempotencyService.StoredResponse> cached = idempotencyService.findCompleted(idempotencyKey, hash);
        if (cached.isPresent()) {
            return toResponse(cached.get(), responseType);
        }

        IdempotencyRecord reservation = idempotencyService.reserve(idempotencyKey, endpoint, hash);
        if (reservation.isCompleted()) {
            return toResponse(new IdempotencyService.StoredResponse(reservation.getResponseStatus(), reservation.getResponseBody()), responseType);
        }

        T result = operation.get();
        String body;
        try {
            body = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to serialize response");
        }
        int status = HttpStatus.OK.value();
        idempotencyService.complete(reservation, status, body);
        return ResponseEntity.status(status).body(result);
    }

    private <T> ResponseEntity<T> toResponse(IdempotencyService.StoredResponse stored, Class<T> responseType) {
        try {
            T body = objectMapper.readValue(stored.body(), responseType);
            return ResponseEntity.status(stored.status()).body(body);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to deserialize stored idempotent response");
        }
    }
}
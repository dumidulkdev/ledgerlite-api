package com.ledgerlite.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlite.domain.IdempotencyRecord;
import com.ledgerlite.exception.ApiException;
import com.ledgerlite.exception.ConflictException;
import com.ledgerlite.repository.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void requireKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
    }

    public String hashRequest(Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to hash request body");
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredResponse> findCompleted(String key, String requestHash) {
        return repository.findByIdempotencyKey(key).flatMap(record -> {
            if (!record.getRequestHash().equals(requestHash)) {
                throw new ConflictException("Idempotency-Key was already used with a different request body");
            }
            if (!record.isCompleted()) {
                return Optional.empty();
            }
            return Optional.of(new StoredResponse(record.getResponseStatus(), record.getResponseBody()));
        });
    }

    @Transactional
    public IdempotencyRecord reserve(String key, String endpoint, String requestHash) {
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            resolve(existing.get(), requestHash);
            return existing.get();
        }
        IdempotencyRecord record = new IdempotencyRecord(
                UUID.randomUUID(),
                key,
                endpoint,
                requestHash,
                null,
                null,
                false,
                Instant.now(clock)
        );
        try {
            return repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            IdempotencyRecord raced = repository.findByIdempotencyKey(key)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Idempotency key collision"));
            resolve(raced, requestHash);
            return raced;
        }
    }

    @Transactional
    public void complete(IdempotencyRecord record, int status, String responseBody) {
        record.markCompleted(status, responseBody);
        repository.save(record);
    }

    private StoredResponse resolve(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new ConflictException("Idempotency-Key was already used with a different request body");
        }
        if (record.isCompleted()) {
            return new StoredResponse(record.getResponseStatus(), record.getResponseBody());
        }
        throw new ApiException(HttpStatus.CONFLICT, "A request with this Idempotency-Key is still processing");
    }

    public record StoredResponse(int status, String body) {
    }
}
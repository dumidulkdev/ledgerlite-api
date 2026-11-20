package com.ledgerlite.controller;

import com.ledgerlite.controller.annotation.RequireIdempotencyKey;
import com.ledgerlite.dto.CreateTransferRequest;
import com.ledgerlite.dto.TransferResponse;
import com.ledgerlite.mapper.TransferMapper;
import com.ledgerlite.service.IdempotentExecutionService;
import com.ledgerlite.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.ledgerlite.config.OpenApiConfig.IDEMPOTENCY_KEY_HEADER;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final TransferMapper transferMapper;
    private final IdempotentExecutionService idempotentExecutionService;

    public TransferController(TransferService transferService,
                              TransferMapper transferMapper,
                              IdempotentExecutionService idempotentExecutionService) {
        this.transferService = transferService;
        this.transferMapper = transferMapper;
        this.idempotentExecutionService = idempotentExecutionService;
    }

    @PostMapping
    @RequireIdempotencyKey
    @Operation(summary = "Transfer funds between wallets in the same currency")
    public ResponseEntity<TransferResponse> transfer(@RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
                                                        @Valid @RequestBody CreateTransferRequest request) {
        return idempotentExecutionService.execute(
                idempotencyKey,
                "/api/transfers",
                request,
                () -> transferMapper.toResponse(transferService.create(request)),
                TransferResponse.class
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer by id")
    public TransferResponse get(@PathVariable UUID id) {
        return transferMapper.toResponse(transferService.get(id));
    }
}
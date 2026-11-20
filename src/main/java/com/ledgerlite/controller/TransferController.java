package com.ledgerlite.controller;

import com.ledgerlite.dto.CreateTransferRequest;
import com.ledgerlite.dto.TransferResponse;
import com.ledgerlite.mapper.TransferMapper;
import com.ledgerlite.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final TransferMapper transferMapper;

    public TransferController(TransferService transferService, TransferMapper transferMapper) {
        this.transferService = transferService;
        this.transferMapper = transferMapper;
    }

    @PostMapping
    @Operation(summary = "Transfer funds between wallets in the same currency")
    public TransferResponse transfer(@Valid @RequestBody CreateTransferRequest request) {
        return transferMapper.toResponse(transferService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer by id")
    public TransferResponse get(@PathVariable UUID id) {
        return transferMapper.toResponse(transferService.get(id));
    }
}

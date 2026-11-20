package com.ledgerlite.controller;

import com.ledgerlite.dto.CreateWalletRequest;
import com.ledgerlite.dto.LedgerEntryResponse;
import com.ledgerlite.dto.MoneyMovementRequest;
import com.ledgerlite.dto.WalletResponse;
import com.ledgerlite.mapper.WalletMapper;
import com.ledgerlite.service.LedgerService;
import com.ledgerlite.service.WalletOperationService;
import com.ledgerlite.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private final WalletOperationService walletOperationService;
    private final LedgerService ledgerService;
    private final WalletMapper walletMapper;

    public WalletController(WalletService walletService,
                            WalletOperationService walletOperationService,
                            LedgerService ledgerService,
                            WalletMapper walletMapper) {
        this.walletService = walletService;
        this.walletOperationService = walletOperationService;
        this.ledgerService = ledgerService;
        this.walletMapper = walletMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a wallet")
    public WalletResponse create(@Valid @RequestBody CreateWalletRequest request) {
        return walletMapper.toResponse(walletService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet by id")
    public WalletResponse get(@PathVariable UUID id) {
        return walletMapper.toResponse(walletService.get(id));
    }

    @GetMapping
    @Operation(summary = "List wallets")
    public Page<WalletResponse> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return walletService.list(pageable).map(walletMapper::toResponse);
    }

    @PostMapping("/{id}/deposits")
    @Operation(summary = "Deposit funds into a wallet", responses = @ApiResponse(responseCode = "200", description = "Deposit applied"))
    public WalletResponse deposit(@PathVariable UUID id, @Valid @RequestBody MoneyMovementRequest request) {
        walletOperationService.deposit(id, request);
        return walletMapper.toResponse(walletService.get(id));
    }

    @PostMapping("/{id}/withdrawals")
    @Operation(summary = "Withdraw funds from a wallet")
    public WalletResponse withdraw(@PathVariable UUID id, @Valid @RequestBody MoneyMovementRequest request) {
        walletOperationService.withdraw(id, request);
        return walletMapper.toResponse(walletService.get(id));
    }

    @GetMapping("/{id}/ledger")
    @Operation(summary = "Paged wallet ledger history (newest first)")
    public Page<LedgerEntryResponse> ledger(@PathVariable UUID id,
                                            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                            Pageable pageable) {
        return ledgerService.walletLedger(id, pageable);
    }
}

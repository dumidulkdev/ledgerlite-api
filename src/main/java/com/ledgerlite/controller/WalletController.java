package com.ledgerlite.controller;

import com.ledgerlite.dto.CreateWalletRequest;
import com.ledgerlite.dto.MoneyMovementRequest;
import com.ledgerlite.dto.WalletResponse;
import com.ledgerlite.mapper.WalletMapper;
import com.ledgerlite.service.IdempotentExecutionService;
import com.ledgerlite.service.LedgerService;
import com.ledgerlite.service.WalletOperationService;
import com.ledgerlite.service.WalletService;
import com.ledgerlite.controller.annotation.RequireIdempotencyKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.ledgerlite.config.OpenApiConfig.IDEMPOTENCY_KEY_HEADER;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private final WalletOperationService walletOperationService;
    private final LedgerService ledgerService;
    private final WalletMapper walletMapper;
    private final IdempotentExecutionService idempotentExecutionService;

    public WalletController(WalletService walletService,
                            WalletOperationService walletOperationService,
                            LedgerService ledgerService,
                            WalletMapper walletMapper,
                            IdempotentExecutionService idempotentExecutionService) {
        this.walletService = walletService;
        this.walletOperationService = walletOperationService;
        this.ledgerService = ledgerService;
        this.walletMapper = walletMapper;
        this.idempotentExecutionService = idempotentExecutionService;
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
    @RequireIdempotencyKey
    @Operation(summary = "Deposit funds into a wallet", responses = @ApiResponse(responseCode = "200", description = "Deposit applied"))
    public ResponseEntity<WalletResponse> deposit(@PathVariable UUID id,
                                                     @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
                                                     @Valid @RequestBody MoneyMovementRequest request) {
        String endpoint = "/api/wallets/" + id + "/deposits";
        return idempotentExecutionService.execute(idempotencyKey, endpoint, request, () -> {
            walletOperationService.deposit(id, request);
            return walletMapper.toResponse(walletService.get(id));
        }, WalletResponse.class);
    }

    @PostMapping("/{id}/withdrawals")
    @RequireIdempotencyKey
    @Operation(summary = "Withdraw funds from a wallet")
    public ResponseEntity<WalletResponse> withdraw(@PathVariable UUID id,
                                                      @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
                                                      @Valid @RequestBody MoneyMovementRequest request) {
        String endpoint = "/api/wallets/" + id + "/withdrawals";
        return idempotentExecutionService.execute(idempotencyKey, endpoint, request, () -> {
            walletOperationService.withdraw(id, request);
            return walletMapper.toResponse(walletService.get(id));
        }, WalletResponse.class);
    }

    @GetMapping("/{id}/ledger")
    @Operation(summary = "Paged wallet ledger history (newest first)")
    public Page<com.ledgerlite.dto.LedgerEntryResponse> ledger(@PathVariable UUID id,
                                                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                                               Pageable pageable) {
        return ledgerService.walletLedger(id, pageable);
    }
}

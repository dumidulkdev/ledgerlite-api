package com.ledgerlite.controller;

import com.ledgerlite.dto.LedgerIntegrityResponse;
import com.ledgerlite.service.IntegrityService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final IntegrityService integrityService;

    public AdminController(IntegrityService integrityService) {
        this.integrityService = integrityService;
    }

    @GetMapping("/ledger/verify")
    @Operation(summary = "Verify wallet balances and double-entry integrity")
    public LedgerIntegrityResponse verify() {
        return integrityService.verify();
    }
}
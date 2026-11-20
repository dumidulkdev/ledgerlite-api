package com.ledgerlite.service;

import com.ledgerlite.domain.LedgerEntry;
import com.ledgerlite.domain.LedgerEntryType;
import com.ledgerlite.dto.LedgerEntryResponse;
import com.ledgerlite.dto.StatementResponse;
import com.ledgerlite.exception.ResourceNotFoundException;
import com.ledgerlite.mapper.LedgerMapper;
import com.ledgerlite.repository.LedgerEntryRepository;
import com.ledgerlite.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StatementService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerMapper ledgerMapper;
    private final MoneyService moneyService;

    public StatementService(WalletRepository walletRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            LedgerMapper ledgerMapper,
                            MoneyService moneyService) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerMapper = ledgerMapper;
        this.moneyService = moneyService;
    }

    @Transactional(readOnly = true)
    public StatementResponse build(UUID walletId, Instant from, Instant to, Pageable pageable) {
        walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        List<LedgerEntry> entriesBefore = ledgerEntryRepository
                .findByWalletIdAndCreatedAtBeforeOrderByCreatedAtDesc(walletId, from);
        BigDecimal opening = computeNet(entriesBefore);

        List<LedgerEntry> periodEntries = ledgerEntryRepository
                .findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(walletId, from, to, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        BigDecimal credits = moneyService.zero();
        BigDecimal debits = moneyService.zero();
        for (LedgerEntry entry : periodEntries) {
            if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                credits = moneyService.add(credits, entry.getAmount());
            } else {
                debits = moneyService.add(debits, entry.getAmount());
            }
        }
        BigDecimal closing = moneyService.subtract(moneyService.add(opening, credits), debits);

        Page<LedgerEntryResponse> pagedEntries = ledgerEntryRepository
                .findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(walletId, from, to, pageable)
                .map(ledgerMapper::toResponse);

        return new StatementResponse(from, to, opening, closing, credits, debits, pagedEntries);
    }

    private BigDecimal computeNet(List<LedgerEntry> entries) {
        BigDecimal net = moneyService.zero();
        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                net = moneyService.add(net, entry.getAmount());
            } else {
                net = moneyService.subtract(net, entry.getAmount());
            }
        }
        return net;
    }

    public Pageable defaultEntriesPage() {
        return PageRequest.of(0, 50);
    }
}
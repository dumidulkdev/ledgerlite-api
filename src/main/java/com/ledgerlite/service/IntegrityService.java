package com.ledgerlite.service;

import com.ledgerlite.domain.Wallet;
import com.ledgerlite.dto.LedgerIntegrityResponse;
import com.ledgerlite.repository.LedgerEntryRepository;
import com.ledgerlite.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IntegrityService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final MoneyService moneyService;

    public IntegrityService(WalletRepository walletRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            MoneyService moneyService) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.moneyService = moneyService;
    }

    @Transactional(readOnly = true)
    public LedgerIntegrityResponse verify() {
        Map<UUID, BigDecimal> ledgerBalances = new HashMap<>();
        for (Object[] row : ledgerEntryRepository.walletLedgerBalances()) {
            UUID walletId = (UUID) row[0];
            BigDecimal balance = ((BigDecimal) row[1]).setScale(MoneyService.SCALE, MoneyService.ROUNDING);
            ledgerBalances.put(walletId, balance);
        }

        List<LedgerIntegrityResponse.WalletMismatch> walletMismatches = new ArrayList<>();
        for (Wallet wallet : walletRepository.findAll()) {
            BigDecimal ledgerBalance = ledgerBalances.getOrDefault(wallet.getId(), moneyService.zero());
            if (wallet.getBalance().compareTo(ledgerBalance) != 0) {
                walletMismatches.add(new LedgerIntegrityResponse.WalletMismatch(
                        wallet.getId(), wallet.getBalance(), ledgerBalance));
            }
        }

        List<LedgerIntegrityResponse.TransactionImbalance> transactionImbalances = new ArrayList<>();
        for (Object[] row : ledgerEntryRepository.transactionNetSums()) {
            UUID transactionId = (UUID) row[0];
            BigDecimal net = ((BigDecimal) row[1]).setScale(MoneyService.SCALE, MoneyService.ROUNDING);
            if (net.compareTo(moneyService.zero()) != 0) {
                transactionImbalances.add(new LedgerIntegrityResponse.TransactionImbalance(transactionId, net));
            }
        }

        boolean healthy = walletMismatches.isEmpty() && transactionImbalances.isEmpty();
        return new LedgerIntegrityResponse(healthy, walletMismatches, transactionImbalances);
    }
}
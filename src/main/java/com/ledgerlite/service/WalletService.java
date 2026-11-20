package com.ledgerlite.service;

import com.ledgerlite.domain.Wallet;
import com.ledgerlite.dto.CreateWalletRequest;
import com.ledgerlite.exception.ResourceNotFoundException;
import com.ledgerlite.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final MoneyService moneyService;

    public WalletService(WalletRepository walletRepository, MoneyService moneyService) {
        this.walletRepository = walletRepository;
        this.moneyService = moneyService;
    }

    @Transactional
    public Wallet create(CreateWalletRequest request) {
        String currency = request.currency().toUpperCase(Locale.ROOT);
        Wallet wallet = new Wallet(UUID.randomUUID(), request.ownerName(), currency, moneyService.zero());
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet get(UUID id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    @Transactional(readOnly = true)
    public Page<Wallet> list(Pageable pageable) {
        return walletRepository.findAll(pageable);
    }
}
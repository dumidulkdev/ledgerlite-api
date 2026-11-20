package com.ledgerlite.service;

import com.ledgerlite.config.RetryProperties;
import com.ledgerlite.domain.LedgerAccountType;
import com.ledgerlite.domain.LedgerEntryType;
import com.ledgerlite.domain.LedgerTransaction;
import com.ledgerlite.domain.LedgerTransactionType;
import com.ledgerlite.domain.Wallet;
import com.ledgerlite.dto.MoneyMovementRequest;
import com.ledgerlite.exception.InsufficientFundsException;
import com.ledgerlite.repository.WalletRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletOperationService {

    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;
    private final MoneyService moneyService;

    public WalletOperationService(WalletRepository walletRepository,
                                  LedgerService ledgerService,
                                  MoneyService moneyService) {
        this.walletRepository = walletRepository;
        this.ledgerService = ledgerService;
        this.moneyService = moneyService;
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttemptsExpression = "${ledgerlite.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${ledgerlite.retry.initial-backoff-ms}",
                    multiplierExpression = "${ledgerlite.retry.multiplier}"
            )
    )
    public LedgerTransaction deposit(UUID walletId, MoneyMovementRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new com.ledgerlite.exception.ResourceNotFoundException("Wallet not found"));
        BigDecimal amount = moneyService.normalize(request.amount());
        wallet.setBalance(moneyService.add(wallet.getBalance(), amount));
        walletRepository.save(wallet);

        return ledgerService.createTransaction(
                LedgerTransactionType.DEPOSIT,
                request.reference(),
                List.of(
                        new LedgerService.EntryDraft(walletId, LedgerAccountType.WALLET, LedgerEntryType.CREDIT, amount),
                        new LedgerService.EntryDraft(null, LedgerAccountType.SYSTEM_CASH, LedgerEntryType.DEBIT, amount)
                )
        );
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttemptsExpression = "${ledgerlite.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${ledgerlite.retry.initial-backoff-ms}",
                    multiplierExpression = "${ledgerlite.retry.multiplier}"
            )
    )
    public LedgerTransaction withdraw(UUID walletId, MoneyMovementRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new com.ledgerlite.exception.ResourceNotFoundException("Wallet not found"));
        BigDecimal amount = moneyService.normalize(request.amount());
        if (moneyService.isLessThan(wallet.getBalance(), amount)) {
            throw new InsufficientFundsException();
        }
        wallet.setBalance(moneyService.subtract(wallet.getBalance(), amount));
        walletRepository.save(wallet);

        return ledgerService.createTransaction(
                LedgerTransactionType.WITHDRAWAL,
                request.reference(),
                List.of(
                        new LedgerService.EntryDraft(walletId, LedgerAccountType.WALLET, LedgerEntryType.DEBIT, amount),
                        new LedgerService.EntryDraft(null, LedgerAccountType.SYSTEM_CASH, LedgerEntryType.CREDIT, amount)
                )
        );
    }
}
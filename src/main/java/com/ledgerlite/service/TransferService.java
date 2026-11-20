package com.ledgerlite.service;

import com.ledgerlite.domain.LedgerAccountType;
import com.ledgerlite.domain.LedgerEntryType;
import com.ledgerlite.domain.LedgerTransaction;
import com.ledgerlite.domain.LedgerTransactionType;
import com.ledgerlite.domain.Transfer;
import com.ledgerlite.domain.Wallet;
import com.ledgerlite.dto.CreateTransferRequest;
import com.ledgerlite.exception.BusinessRuleException;
import com.ledgerlite.exception.InsufficientFundsException;
import com.ledgerlite.exception.ResourceNotFoundException;
import com.ledgerlite.repository.TransferRepository;
import com.ledgerlite.repository.WalletRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final LedgerService ledgerService;
    private final MoneyService moneyService;
    private final Clock clock;

    public TransferService(WalletRepository walletRepository,
                           TransferRepository transferRepository,
                           LedgerService ledgerService,
                           MoneyService moneyService,
                           Clock clock) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.ledgerService = ledgerService;
        this.moneyService = moneyService;
        this.clock = clock;
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
    public Transfer create(CreateTransferRequest request) {
        if (request.sourceWalletId().equals(request.targetWalletId())) {
            throw new BusinessRuleException("Cannot transfer to the same wallet");
        }
        Wallet source = walletRepository.findById(request.sourceWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Source wallet not found"));
        Wallet target = walletRepository.findById(request.targetWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Target wallet not found"));
        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new BusinessRuleException("Currency mismatch between wallets");
        }

        BigDecimal amount = moneyService.normalize(request.amount());
        if (moneyService.isLessThan(source.getBalance(), amount)) {
            throw new InsufficientFundsException();
        }

        source.setBalance(moneyService.subtract(source.getBalance(), amount));
        target.setBalance(moneyService.add(target.getBalance(), amount));
        walletRepository.save(source);
        walletRepository.save(target);

        LedgerTransaction ledgerTransaction = ledgerService.createTransaction(
                LedgerTransactionType.TRANSFER,
                request.reference(),
                List.of(
                        new LedgerService.EntryDraft(source.getId(), LedgerAccountType.WALLET, LedgerEntryType.DEBIT, amount),
                        new LedgerService.EntryDraft(target.getId(), LedgerAccountType.WALLET, LedgerEntryType.CREDIT, amount)
                )
        );

        Instant now = Instant.now(clock);
        Transfer transfer = new Transfer(
                UUID.randomUUID(),
                source.getId(),
                target.getId(),
                amount,
                request.reference(),
                ledgerTransaction.getId(),
                now
        );
        transferRepository.save(transfer);


        return transfer;
    }

    @Transactional(readOnly = true)
    public Transfer get(UUID id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
    }
}
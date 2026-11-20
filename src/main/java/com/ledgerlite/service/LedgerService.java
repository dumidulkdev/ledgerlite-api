package com.ledgerlite.service;

import com.ledgerlite.domain.LedgerAccountType;
import com.ledgerlite.domain.LedgerEntry;
import com.ledgerlite.domain.LedgerEntryType;
import com.ledgerlite.domain.LedgerTransaction;
import com.ledgerlite.domain.LedgerTransactionType;
import com.ledgerlite.dto.LedgerEntryResponse;
import com.ledgerlite.exception.BusinessRuleException;
import com.ledgerlite.mapper.LedgerMapper;
import com.ledgerlite.repository.LedgerEntryRepository;
import com.ledgerlite.repository.LedgerTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerMapper ledgerMapper;
    private final MoneyService moneyService;
    private final Clock clock;

    public LedgerService(LedgerTransactionRepository transactionRepository,
                         LedgerEntryRepository entryRepository,
                         LedgerMapper ledgerMapper,
                         MoneyService moneyService,
                         Clock clock) {
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.ledgerMapper = ledgerMapper;
        this.moneyService = moneyService;
        this.clock = clock;
    }

    @Transactional
    public LedgerTransaction createTransaction(LedgerTransactionType type, String reference, List<EntryDraft> drafts) {
        validateBalanced(drafts);
        Instant now = Instant.now(clock);
        UUID transactionId = UUID.randomUUID();
        LedgerTransaction transaction = new LedgerTransaction(transactionId, type, reference, now);
        transactionRepository.save(transaction);

        List<LedgerEntry> entries = new ArrayList<>();
        for (EntryDraft draft : drafts) {
            entries.add(new LedgerEntry(
                    UUID.randomUUID(),
                    transactionId,
                    draft.walletId(),
                    draft.accountType(),
                    draft.entryType(),
                    moneyService.normalize(draft.amount()),
                    now
            ));
        }
        entryRepository.saveAll(entries);
        return transaction;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> walletLedger(UUID walletId, Pageable pageable) {
        return entryRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(ledgerMapper::toResponse);
    }

    private void validateBalanced(List<EntryDraft> drafts) {
        BigDecimal credits = moneyService.zero();
        BigDecimal debits = moneyService.zero();
        for (EntryDraft draft : drafts) {
            BigDecimal amount = moneyService.normalize(draft.amount());
            if (draft.entryType() == LedgerEntryType.CREDIT) {
                credits = moneyService.add(credits, amount);
            } else {
                debits = moneyService.add(debits, amount);
            }
        }
        if (credits.compareTo(debits) != 0) {
            throw new BusinessRuleException("Ledger transaction is not balanced");
        }
    }

    public record EntryDraft(UUID walletId, LedgerAccountType accountType, LedgerEntryType entryType, BigDecimal amount) {
    }
}
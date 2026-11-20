package com.ledgerlite.service;

import com.ledgerlite.domain.LedgerAccountType;
import com.ledgerlite.domain.LedgerEntryType;
import com.ledgerlite.domain.LedgerTransaction;
import com.ledgerlite.domain.LedgerTransactionType;
import com.ledgerlite.exception.BusinessRuleException;
import com.ledgerlite.mapper.LedgerMapper;
import com.ledgerlite.repository.LedgerEntryRepository;
import com.ledgerlite.repository.LedgerTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerTransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository entryRepository;

    @Mock
    private LedgerMapper ledgerMapper;

    @Mock
    private MoneyService moneyService;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2024-06-01T12:00:00Z"), ZoneOffset.UTC);
        ledgerService = new LedgerService(transactionRepository, entryRepository, ledgerMapper, moneyService, clock);
        when(moneyService.zero()).thenReturn(new BigDecimal("0.00"));
        when(moneyService.normalize(any())).thenAnswer(invocation -> {
            BigDecimal value = invocation.getArgument(0);
            return value.setScale(2);
        });
        when(moneyService.add(any(), any())).thenAnswer(invocation -> {
            BigDecimal left = invocation.getArgument(0);
            BigDecimal right = invocation.getArgument(1);
            return left.add(right).setScale(2);
        });
    }

    @Test
    void createsBalancedTransaction() {
        UUID walletId = UUID.randomUUID();
        List<LedgerService.EntryDraft> drafts = List.of(
                new LedgerService.EntryDraft(walletId, LedgerAccountType.WALLET, LedgerEntryType.CREDIT, new BigDecimal("25.00")),
                new LedgerService.EntryDraft(null, LedgerAccountType.SYSTEM_CASH, LedgerEntryType.DEBIT, new BigDecimal("25.00"))
        );
        when(transactionRepository.save(any(LedgerTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.createTransaction(LedgerTransactionType.DEPOSIT, "payroll", drafts);

        verify(transactionRepository).save(any(LedgerTransaction.class));
        verify(entryRepository).saveAll(any());
    }

    @Test
    void rejectsUnbalancedEntries() {
        UUID walletId = UUID.randomUUID();
        List<LedgerService.EntryDraft> drafts = List.of(
                new LedgerService.EntryDraft(walletId, LedgerAccountType.WALLET, LedgerEntryType.CREDIT, new BigDecimal("25.00")),
                new LedgerService.EntryDraft(null, LedgerAccountType.SYSTEM_CASH, LedgerEntryType.DEBIT, new BigDecimal("10.00"))
        );

        assertThrows(BusinessRuleException.class,
                () -> ledgerService.createTransaction(LedgerTransactionType.DEPOSIT, "bad", drafts));
    }
}

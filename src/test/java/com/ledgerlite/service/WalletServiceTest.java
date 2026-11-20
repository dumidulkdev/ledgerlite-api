package com.ledgerlite.service;

import com.ledgerlite.domain.Wallet;
import com.ledgerlite.dto.CreateWalletRequest;
import com.ledgerlite.exception.ResourceNotFoundException;
import com.ledgerlite.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MoneyService moneyService;

    @InjectMocks
    private WalletService walletService;

    @Test
    void createsWalletWithUppercaseCurrencyAndZeroBalance() {
        when(moneyService.zero()).thenReturn(new BigDecimal("0.00"));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.create(new CreateWalletRequest("Taylor Brooks", "usd"));

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        assertEquals("USD", captor.getValue().getCurrency());
    }

    @Test
    void loadsWalletById() {
        UUID id = UUID.randomUUID();
        Wallet wallet = new Wallet(id, "Owner", "EUR", new BigDecimal("12.50"));
        when(walletRepository.findById(id)).thenReturn(Optional.of(wallet));

        assertEquals(wallet, walletService.get(id));
    }

    @Test
    void throwsWhenWalletMissing() {
        UUID id = UUID.randomUUID();
        when(walletRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.get(id));
    }

    @Test
    void listsWalletsWithPagination() {
        Pageable pageable = PageRequest.of(0, 5);
        Wallet wallet = new Wallet(UUID.randomUUID(), "Owner", "USD", new BigDecimal("0.00"));
        Page<Wallet> page = new PageImpl<>(List.of(wallet), pageable, 1);
        when(walletRepository.findAll(pageable)).thenReturn(page);

        assertEquals(1, walletService.list(pageable).getTotalElements());
    }
}

package com.ledgerlite.mapper;

import com.ledgerlite.domain.Wallet;
import com.ledgerlite.dto.WalletResponse;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getOwnerName(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
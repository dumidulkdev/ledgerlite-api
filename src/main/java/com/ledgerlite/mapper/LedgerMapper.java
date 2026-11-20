package com.ledgerlite.mapper;

import com.ledgerlite.domain.LedgerEntry;
import com.ledgerlite.dto.LedgerEntryResponse;
import org.springframework.stereotype.Component;

@Component
public class LedgerMapper {

    public LedgerEntryResponse toResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransactionId(),
                entry.getWalletId(),
                entry.getAccountType(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getCreatedAt()
        );
    }
}
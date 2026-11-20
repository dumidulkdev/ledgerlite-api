package com.ledgerlite.mapper;

import com.ledgerlite.domain.Transfer;
import com.ledgerlite.dto.TransferResponse;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceWalletId(),
                transfer.getTargetWalletId(),
                transfer.getAmount(),
                transfer.getReference(),
                transfer.getCreatedAt()
        );
    }
}
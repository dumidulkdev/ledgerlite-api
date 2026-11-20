package com.ledgerlite.dto;

import java.util.List;

public record LedgerIntegrityResponse(
        boolean healthy,
        List<WalletMismatch> walletMismatches,
        List<TransactionImbalance> transactionImbalances
) {
    public record WalletMismatch(java.util.UUID walletId, java.math.BigDecimal storedBalance,
                                 java.math.BigDecimal ledgerBalance) {
    }

    public record TransactionImbalance(java.util.UUID transactionId, java.math.BigDecimal netAmount) {
    }
}
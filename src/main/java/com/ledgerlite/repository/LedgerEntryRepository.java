package com.ledgerlite.repository;

import com.ledgerlite.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    Page<LedgerEntry> findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID walletId, Instant from, Instant to, Pageable pageable);

    List<LedgerEntry> findByWalletIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID walletId, Instant point);

    @Query("""
            select coalesce(sum(case when e.entryType = com.ledgerlite.domain.LedgerEntryType.CREDIT then e.amount else 0 end), 0),
                   coalesce(sum(case when e.entryType = com.ledgerlite.domain.LedgerEntryType.DEBIT then e.amount else 0 end), 0)
            from LedgerEntry e
            where e.walletId = :walletId and e.accountType = com.ledgerlite.domain.LedgerAccountType.WALLET
              and e.createdAt >= :from and e.createdAt < :to
            """)
    Object[] sumCreditsAndDebits(@Param("walletId") UUID walletId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select coalesce(sum(case when e.entryType = com.ledgerlite.domain.LedgerEntryType.CREDIT then e.amount else -e.amount end), 0)
            from LedgerEntry e
            where e.walletId = :walletId and e.accountType = com.ledgerlite.domain.LedgerAccountType.WALLET
              and e.createdAt < :point
            """)
    BigDecimal balanceBefore(@Param("walletId") UUID walletId, @Param("point") Instant point);

    @Query("""
            select e.walletId,
                   coalesce(sum(case when e.entryType = com.ledgerlite.domain.LedgerEntryType.CREDIT then e.amount else -e.amount end), 0)
            from LedgerEntry e
            where e.walletId is not null and e.accountType = com.ledgerlite.domain.LedgerAccountType.WALLET
            group by e.walletId
            """)
    List<Object[]> walletLedgerBalances();

    @Query("""
            select e.transactionId,
                   coalesce(sum(case when e.entryType = com.ledgerlite.domain.LedgerEntryType.CREDIT then e.amount else -e.amount end), 0)
            from LedgerEntry e
            group by e.transactionId
            """)
    List<Object[]> transactionNetSums();
}
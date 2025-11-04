package org.pix.wallet.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.pix.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query(value = """
        SELECT 
            COALESCE(SUM(
                CASE 
                    WHEN operationType = 'DEPOSIT' THEN amount
                    WHEN operationType = 'WITHDRAW' THEN -amount
                    ELSE 0
                END
            ), 0) AS balance
        FROM ledger_entry
        WHERE wallet_id = :walletId;
    """, nativeQuery = true)
    Optional<BigDecimal> findCurrentBalanceByWalletId(UUID walletId);


    @Query(value = """
        SELECT 
            COALESCE(SUM(
                CASE 
                    WHEN operationType = 'DEPOSIT' THEN amount
                    WHEN operationType = 'WITHDRAW' THEN -amount
                    ELSE 0
                END
            ), 0) AS balance
        FROM ledger_entry
        WHERE wallet_id = :walletId and created_at <= :asOf;
    """, nativeQuery = true)
    Optional<BigDecimal> findHistoricalBalance(UUID walletId, Instant asOf);

    
}
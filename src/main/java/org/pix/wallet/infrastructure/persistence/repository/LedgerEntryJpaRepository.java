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

    /**
     * Calculates real/actual balance (accounting balance).
     * Does NOT consider reserved funds - only confirmed operations (DEPOSIT, WITHDRAW).
     * 
     * Formula: SUM(DEPOSIT - WITHDRAW)
     * Note: RESERVED and UNRESERVED operations are IGNORED for accounting purposes
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(
                CASE 
                    WHEN operation_type = 'DEPOSIT' THEN amount
                    WHEN operation_type = 'WITHDRAW' THEN -amount
                    ELSE 0
                END
            ), 0) AS real_balance
        FROM ledger_entry
        WHERE wallet_id = :walletId;
    """, nativeQuery = true)
    Optional<BigDecimal> findCurrentBalanceByWalletId(UUID walletId);

    /**
     * Calculates available balance considering reserved funds.
     * Available balance = Real balance - Reserved funds
     * 
     * Formula: SUM(DEPOSIT - WITHDRAW - RESERVED + UNRESERVED)
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(
                CASE 
                    WHEN operation_type = 'DEPOSIT' THEN amount
                    WHEN operation_type = 'WITHDRAW' THEN -amount
                    WHEN operation_type = 'RESERVED' THEN -amount
                    WHEN operation_type = 'UNRESERVED' THEN amount
                    ELSE 0
                END
            ), 0) AS available_balance
        FROM ledger_entry
        WHERE wallet_id = :walletId;
    """, nativeQuery = true)
    Optional<BigDecimal> findAvailableBalance(UUID walletId);

    /**
     * Calculates historical available balance at a specific point in time.
     * Includes all operations (DEPOSIT, WITHDRAW, RESERVED, UNRESERVED) up to the specified timestamp.
     * 
     * Formula: SUM(DEPOSIT - WITHDRAW - RESERVED + UNRESERVED) WHERE created_at <= asOf
     */
    @Query(value = """
        SELECT 
            COALESCE(SUM(
                CASE 
                    WHEN operation_type = 'DEPOSIT' THEN amount
                    WHEN operation_type = 'WITHDRAW' THEN -amount
                    WHEN operation_type = 'RESERVED' THEN -amount
                    WHEN operation_type = 'UNRESERVED' THEN amount
                    ELSE 0
                END
            ), 0) AS available_balance
        FROM ledger_entry
        WHERE wallet_id = :walletId AND created_at <= :asOf;
    """, nativeQuery = true)
    Optional<BigDecimal> findHistoricalBalance(UUID walletId, Instant asOf);

    
}
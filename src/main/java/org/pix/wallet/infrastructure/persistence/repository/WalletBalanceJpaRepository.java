package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.WalletBalanceEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WalletBalanceJpaRepository extends JpaRepository<WalletBalanceEntity, UUID> {
    Optional<WalletBalanceEntity> findByWalletId(UUID walletId);

    @Query(value = """
        WITH upsert AS (
            INSERT INTO wallet_balance (wallet_id, balance, updated_at)
            VALUES (:walletId, :amount, NOW())
            ON CONFLICT (wallet_id)
            DO UPDATE SET balance = wallet_balance.balance + EXCLUDED.balance,
                          updated_at = NOW()
            RETURNING balance
        )
        SELECT balance FROM upsert
        """, nativeQuery = true)
    BigDecimal upsertAndIncrement(UUID walletId, BigDecimal amount);

}
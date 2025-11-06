package org.pix.wallet.application.port.out;

import java.util.Optional;
import java.math.BigDecimal;

public interface LedgerEntryRepositoryPort {
    boolean existsByIdempotencyKey(String key);

    String deposit(String walletId, BigDecimal amount, String idempotencyKey);

    String withdraw(String walletId, BigDecimal amount, String idempotencyKey);

    /**
     * Reserves (blocks) funds for a PENDING transfer.
     * The reserved amount is subtracted from available balance but not from real balance.
     * 
     * @param walletId ID of the wallet
     * @param amount Amount to reserve (must be positive)
     * @param idempotencyKey Unique key for idempotent operation
     * @return ID of the created ledger entry
     */
    String reserve(String walletId, BigDecimal amount, String idempotencyKey);

    /**
     * Unreserves (releases) previously reserved funds.
     * Called when a PENDING transfer is CONFIRMED or REJECTED.
     * 
     * @param walletId ID of the wallet
     * @param amount Amount to unreserve (must match reserved amount)
     * @param idempotencyKey Unique key for idempotent operation
     * @return ID of the created ledger entry
     */
    String unreserve(String walletId, BigDecimal amount, String idempotencyKey);

    Optional<BigDecimal> getBalanceAsOf(String walletId, java.time.Instant asOf);

    Optional<BigDecimal> getCurrentBalance(String walletId);

    /**
     * Gets the available balance (real balance minus reserved funds).
     * This is the amount that can be used for new operations.
     * 
     * @param walletId ID of the wallet
     * @return Available balance
     */
    Optional<BigDecimal> getAvailableBalance(String walletId);
}


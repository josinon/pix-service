package org.pix.wallet.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepositoryPort {
    
    /**
     * Check if a transfer with the given idempotency key already exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find transfer by idempotency key
     */
    Optional<TransferResult> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find transfer by end-to-end ID
     */
    Optional<TransferResult> findByEndToEndId(String endToEndId);
    
    /**
     * Save a new transfer
     */
    TransferResult save(TransferCommand command);
    
    /**
     * Update transfer status
     */
    void updateStatus(String endToEndId, String status, int currentVersion);
    
    record TransferCommand(
        String endToEndId,
        String fromWalletId,
        String toWalletId,
        java.math.BigDecimal amount,
        String currency,
        String status,
        String idempotencyKey
    ) {}
    
    record TransferResult(
        UUID id,
        String endToEndId,
        String fromWalletId,
        String toWalletId,
        java.math.BigDecimal amount,
        String currency,
        String status,
        int version
    ) {}
}

package org.pix.wallet.application.service;

import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Centralized validator for common wallet operations.
 * Reduces code duplication across services.
 */
@Component
public class WalletOperationValidator {
    
    private final WalletRepositoryPort walletRepositoryPort;
    
    public WalletOperationValidator(WalletRepositoryPort walletRepositoryPort) {
        this.walletRepositoryPort = walletRepositoryPort;
    }
    
    /**
     * Validates that amount is positive.
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
    }
    
    /**
     * Validates that idempotency key is not null or blank.
     */
    public void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key required");
        }
    }
    
    /**
     * Validates wallet exists and returns it.
     */
    public Wallet validateAndGetWallet(UUID walletId) {
        return walletRepositoryPort.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
    }
    
    /**
     * Validates wallet exists, is active, and returns it.
     */
    public Wallet validateAndGetActiveWallet(UUID walletId) {
        Wallet wallet = validateAndGetWallet(walletId);
        
        if (wallet.status() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active: " + walletId);
        }
        
        return wallet;
    }
}

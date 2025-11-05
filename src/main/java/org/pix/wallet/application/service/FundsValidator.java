package org.pix.wallet.application.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.domain.exception.InsufficientFundsException;
import org.springframework.stereotype.Component;

/**
 * Validator focused on balance-related business rules.
 * Keeps WalletOperationValidator free from ledger concerns (SRP).
 */
@Component
public class FundsValidator {

    private final LedgerEntryRepositoryPort ledgerEntryRepositoryPort;

    public FundsValidator(LedgerEntryRepositoryPort ledgerEntryRepositoryPort) {
        this.ledgerEntryRepositoryPort = ledgerEntryRepositoryPort;
    }

    /**
     * Ensures the wallet has at least the requested amount.
     * Returns current balance for optional logging / metrics.
     */
    public BigDecimal ensureSufficientFunds(UUID walletId, BigDecimal requestedAmount) {
        BigDecimal current = ledgerEntryRepositoryPort.getCurrentBalance(walletId.toString())
                .orElse(BigDecimal.ZERO);
        if (current.compareTo(requestedAmount) < 0) {
            throw new InsufficientFundsException(current, requestedAmount);
        }
        return current;
    }
}

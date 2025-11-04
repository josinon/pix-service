package org.pix.wallet.application.port.out;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.Optional;

public interface WalletBalanceRepositoryPort {
    Optional<BigDecimal> findCurrentBalance(UUID walletId);
    BigDecimal incrementBalance(UUID walletId, BigDecimal amount); // retorna novo saldo
}
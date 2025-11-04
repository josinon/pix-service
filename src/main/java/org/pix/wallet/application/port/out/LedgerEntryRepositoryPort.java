package org.pix.wallet.application.port.out;

import java.util.Optional;
import java.math.BigDecimal;

public interface LedgerEntryRepositoryPort {
    boolean existsByIdempotencyKey(String key);

    String deposit(String walletId, BigDecimal amount, String idempotencyKey);

    String withdraw(String walletId, BigDecimal amount, String idempotencyKey);

    Optional<BigDecimal> getBalanceAsOf(String walletId, java.time.Instant asOf);

    Optional<BigDecimal> getCurrentBalance(String walletId);
}

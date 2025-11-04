package org.pix.wallet.application.port.out;

import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

public interface LedgerEntryRepositoryPort {
    boolean existsByIdempotencyKey(String key);

    UUID deposit(UUID walletId, BigDecimal amount, String idempotencyKey);

    UUID withdraw(UUID walletId, BigDecimal amount, String idempotencyKey);

    Optional<BigDecimal> getBalanceAsOf(UUID walletId, java.time.Instant asOf);

    Optional<BigDecimal> getCurrentBalance(UUID walletId);
}

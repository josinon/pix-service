package org.pix.wallet.application.port.out;

import java.util.UUID;
import java.math.BigDecimal;

public interface LedgerEntryRepositoryPort {
    boolean existsByIdempotencyKey(String key);
    UUID appendDeposit(UUID walletId, BigDecimal amount, String idempotencyKey);
}

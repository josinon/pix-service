package org.pix.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface GetBalanceUseCase {
    BigDecimal getBalance(UUID walletId);
}
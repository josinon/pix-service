package org.pix.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface DepositFundsUseCase {

    Result execute(Command command);

    record Command(UUID walletId, BigDecimal amount, String idempotencyKey) { }

    record Result(UUID walletId, BigDecimal previousBalance, BigDecimal amount, BigDecimal newBalance) { }
}
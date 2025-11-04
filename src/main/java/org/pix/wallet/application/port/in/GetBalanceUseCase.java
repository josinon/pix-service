package org.pix.wallet.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface GetBalanceUseCase {

  public Result execute(Command command);

  public record Command(UUID walletId, Instant at) {}

  public record Result(UUID walletId, java.math.BigDecimal balance) {}


}
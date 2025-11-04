package org.pix.wallet.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositResponse(
        UUID walletId,
        BigDecimal previousBalance,
        BigDecimal amount,
        BigDecimal newBalance,
        boolean idempotent
) { }

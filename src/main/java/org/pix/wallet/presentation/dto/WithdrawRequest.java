package org.pix.wallet.presentation.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

public record WithdrawRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be > 0")
        BigDecimal amount
) { }
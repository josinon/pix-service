package org.pix.wallet.presentation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class CreateWalletRequest {
    @NotNull @DecimalMin("0.00")
    public BigDecimal initialAmount;
}

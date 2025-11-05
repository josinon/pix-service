package org.pix.wallet.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PixTransferRequest(
    @NotNull(message = "From wallet ID is required")
    UUID fromWalletId,
    
    @NotNull(message = "To PIX key is required")
    @NotBlank(message = "To PIX key cannot be blank")
    String toPixKey,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount
) {}

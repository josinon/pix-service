package org.pix.wallet.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixTransferRequest {
    
    @NotNull(message = "From wallet ID is required")
    private UUID fromWalletId;
    
    @NotNull(message = "To PIX key is required")
    @NotBlank(message = "To PIX key cannot be blank")
    private String toPixKey;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}

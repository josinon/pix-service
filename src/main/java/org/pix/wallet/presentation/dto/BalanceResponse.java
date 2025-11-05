package org.pix.wallet.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Balance response exposed to the API layer. Extracted from GetBalanceUseCase.Result
 * to provide a stable explicit schema for OpenAPI generation (avoids wildcard generics
 * that can break springdoc).
 */
public record BalanceResponse(
        UUID walletId,
        BigDecimal balance
) {}

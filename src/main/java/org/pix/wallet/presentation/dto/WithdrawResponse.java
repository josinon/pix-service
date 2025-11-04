package org.pix.wallet.presentation.dto;

import java.util.UUID;

public record WithdrawResponse(
        UUID walletId,
        String idempotenceKey
) { }

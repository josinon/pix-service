package org.pix.wallet.presentation.dto;

import java.util.UUID;

public record CreatePixKeyResponse(
        UUID id,
        String type,
        String value,
        String status
) {}
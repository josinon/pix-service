package org.pix.wallet.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePixKeyRequest(
        @NotBlank String type,
        @Size(max = 120) String value    // pode ser vazio para RANDOM
) {}

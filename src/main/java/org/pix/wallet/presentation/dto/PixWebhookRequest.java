package org.pix.wallet.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PixWebhookRequest(
    @NotNull(message = "End to end ID is required")
    String endToEndId,
    
    @NotNull(message = "Event ID is required")
    String eventId,
    
    @NotNull(message = "Event type is required")
    String eventType,
    
    @NotNull(message = "Occurred at is required")
    Instant occurredAt
) {}

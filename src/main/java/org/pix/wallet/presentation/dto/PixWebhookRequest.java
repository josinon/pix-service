package org.pix.wallet.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixWebhookRequest {
    
    @NotNull(message = "End to end ID is required")
    private String endToEndId;
    
    @NotNull(message = "Event ID is required")
    private String eventId;
    
    @NotNull(message = "Event type is required")
    private String eventType;
    
    @NotNull(message = "Occurred at is required")
    private Instant occurredAt;
}

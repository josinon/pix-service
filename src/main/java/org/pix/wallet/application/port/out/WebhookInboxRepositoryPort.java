package org.pix.wallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface WebhookInboxRepositoryPort {
    
    /**
     * Check if webhook event was already processed
     */
    boolean existsByEventId(String eventId);
    
    /**
     * Save webhook event to inbox
     */
    void save(WebhookEvent event);
    
    record WebhookEvent(
        UUID id,
        String endToEndId,
        String eventId,
        String eventType,
        Instant occurredAt,
        Instant processedAt
    ) {}
}

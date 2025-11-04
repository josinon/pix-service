package org.pix.wallet.application.port.in;

import java.time.Instant;

public interface ProcessPixWebhookUseCase {
    
    void execute(Command command);
    
    record Command(
        String endToEndId,
        String eventId,
        String eventType,
        Instant occurredAt
    ) {}
}

package org.pix.wallet.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pix.wallet.application.port.in.ProcessPixWebhookUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PixWebhookService implements ProcessPixWebhookUseCase {

    // TODO: Inject required ports (WebhookInboxRepository, TransferRepository, etc)
    
    @Override
    @Transactional
    public void execute(Command command) {
        log.info("Processing PIX webhook - endToEndId: {}, eventId: {}, eventType: {}, occurredAt: {}", 
                 command.endToEndId(), command.eventId(), command.eventType(), command.occurredAt());
        
        // Validations
        if (command.eventId() == null || command.eventId().isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        
        if (command.endToEndId() == null || command.endToEndId().isBlank()) {
            throw new IllegalArgumentException("End to end ID is required");
        }
        
        if (command.eventType() == null || command.eventType().isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        // TODO: Implement business logic:
        // 1. Check idempotency by eventId (if already processed, return/ignore)
        // 2. Find transfer by endToEndId
        // 3. Process webhook based on eventType:
        //    - CONFIRMED: Update transfer status and apply to wallets
        //    - REJECTED: Update transfer status and reverse if needed
        //    - PENDING: Keep as pending
        // 4. Save webhook event to inbox table
        // 5. Update transfer version (optimistic locking)
        // 6. Save idempotency record with eventId
        
        log.info("PIX webhook processed successfully - eventId: {}", command.eventId());
    }
}

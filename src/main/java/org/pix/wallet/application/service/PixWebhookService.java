package org.pix.wallet.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pix.wallet.application.port.in.ProcessPixWebhookUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.pix.wallet.application.port.out.WebhookInboxRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PixWebhookService implements ProcessPixWebhookUseCase {

    private final WebhookInboxRepositoryPort webhookInboxRepositoryPort;
    private final TransferRepositoryPort transferRepositoryPort;
    private final LedgerEntryRepositoryPort ledgerEntryRepositoryPort;
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(Command command) {
        log.info("Processing PIX webhook - endToEndId: {}, eventId: {}, eventType: {}, occurredAt: {}", 
                 command.endToEndId(), command.eventId(), command.eventType(), command.occurredAt());
        
        // 1. Validations
        validateCommand(command);
        
        // 2. Check idempotency by eventId - if already processed, return/ignore
        if (webhookInboxRepositoryPort.existsByEventId(command.eventId())) {
            log.info("Webhook event already processed: {}", command.eventId());
            return;
        }
        
        // 3. Find transfer by endToEndId
        TransferRepositoryPort.TransferResult transfer = transferRepositoryPort.findByEndToEndId(command.endToEndId())
            .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + command.endToEndId()));
        
        log.info("Transfer found - id: {}, status: {}, fromWallet: {}, toWallet: {}, amount: {}", 
                 transfer.id(), transfer.status(), transfer.fromWalletId(), transfer.toWalletId(), transfer.amount());
        
        // 4. Process webhook based on eventType
        String newStatus = processWebhookEvent(command.eventType(), transfer);
        
        // 5. Update transfer status (with optimistic locking)
        try {
            transferRepositoryPort.updateStatus(command.endToEndId(), newStatus, transfer.version());
            log.info("Transfer status updated - endToEndId: {}, oldStatus: {}, newStatus: {}", 
                     command.endToEndId(), transfer.status(), newStatus);
        } catch (Exception e) {
            log.error("Failed to update transfer status (possible concurrent update): {}", e.getMessage());
            throw new IllegalStateException("Transfer was modified by another process", e);
        }
        
        // 6. Save webhook event to inbox table
        var webhookEvent = new WebhookInboxRepositoryPort.WebhookEvent(
            command.endToEndId(),
            command.eventId(),
            command.eventType(),
            command.occurredAt(),
            Instant.now()
        );
        
        webhookInboxRepositoryPort.save(webhookEvent);
        
        log.info("PIX webhook processed successfully - eventId: {}, endToEndId: {}, newStatus: {}", 
                 command.eventId(), command.endToEndId(), newStatus);
    }
    
    private void validateCommand(Command command) {
        if (command.eventId() == null || command.eventId().isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        
        if (command.endToEndId() == null || command.endToEndId().isBlank()) {
            throw new IllegalArgumentException("End to end ID is required");
        }
        
        if (command.eventType() == null || command.eventType().isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        if (command.occurredAt() == null) {
            throw new IllegalArgumentException("Occurred at is required");
        }
    }
    
    private String processWebhookEvent(String eventType, TransferRepositoryPort.TransferResult transfer) {
        return switch (eventType.toUpperCase()) {
            case "CONFIRMED" -> {
                log.info("Processing CONFIRMED event - applying transfer to wallets");
                applyTransferToWallets(transfer);
                yield "CONFIRMED";
            }
            case "REJECTED" -> {
                log.info("Processing REJECTED event - transfer will not be applied");
                // If transfer was already applied (shouldn't happen), we'd need to reverse it
                // For now, we just mark as rejected
                yield "REJECTED";
            }
            case "PENDING" -> {
                log.info("Processing PENDING event - keeping status as pending");
                yield "PENDING";
            }
            default -> {
                log.warn("Unknown event type: {}, keeping current status", eventType);
                yield transfer.status();
            }
        };
    }
    
    private void applyTransferToWallets(TransferRepositoryPort.TransferResult transfer) {
        // Only apply if not already confirmed
        if ("CONFIRMED".equals(transfer.status())) {
            log.info("Transfer already confirmed, skipping wallet operations");
            return;
        }
        
        String idempotencyKey = transfer.endToEndId() + "-apply";
        
        // Check if already applied using ledger idempotency
        if (ledgerEntryRepositoryPort.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Transfer already applied to wallets (idempotent), skipping");
            return;
        }
        
        // Debit from source wallet
        ledgerEntryRepositoryPort.withdraw(
            transfer.fromWalletId(), 
            transfer.amount(), 
            idempotencyKey + "-debit"
        );
        
        log.info("Debited {} from wallet {}", transfer.amount(), transfer.fromWalletId());
        
        // Credit to destination wallet
        ledgerEntryRepositoryPort.deposit(
            transfer.toWalletId(), 
            transfer.amount(), 
            idempotencyKey + "-credit"
        );
        
        log.info("Credited {} to wallet {}", transfer.amount(), transfer.toWalletId());
    }
}

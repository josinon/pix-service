package org.pix.wallet.application.service;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.pix.wallet.application.port.in.ProcessPixWebhookUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.pix.wallet.application.port.out.WebhookInboxRepositoryPort;
import org.pix.wallet.domain.validator.TransferValidator;
import org.pix.wallet.infrastructure.observability.ObservabilityContext;
import org.pix.wallet.domain.exception.InvalidTransferStatusTransitionException;
import org.pix.wallet.infrastructure.observability.MetricsService;
import org.pix.wallet.infrastructure.observability.Traced;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
public class PixWebhookService implements ProcessPixWebhookUseCase {

    private final WebhookInboxRepositoryPort webhookInboxRepositoryPort;
    private final TransferRepositoryPort transferRepositoryPort;
    private final LedgerEntryRepositoryPort ledgerEntryRepositoryPort;
    private final TransferValidator transferValidator;
    private final MetricsService metricsService;
    
    public PixWebhookService(
            WebhookInboxRepositoryPort webhookInboxRepositoryPort,
            TransferRepositoryPort transferRepositoryPort,
            LedgerEntryRepositoryPort ledgerEntryRepositoryPort,
            TransferValidator transferValidator,
            MetricsService metricsService) {
        this.webhookInboxRepositoryPort = webhookInboxRepositoryPort;
        this.transferRepositoryPort = transferRepositoryPort;
        this.ledgerEntryRepositoryPort = ledgerEntryRepositoryPort;
        this.transferValidator = transferValidator;
        this.metricsService = metricsService;
    }
    
    @Override
    @Traced(operation = "pix.webhook.process", description = "Process PIX webhook")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(Command command) {
        ObservabilityContext.setOperation("PIX_WEBHOOK_PROCESS");
        ObservabilityContext.setEndToEndId(command.endToEndId());
        ObservabilityContext.setEventId(command.eventId());
        
        Timer.Sample metricsTimer = metricsService.startWebhookProcessing();
        
        metricsService.recordWebhookReceived(command.eventType());
        
        try {
            log.info("Processing PIX webhook", 
                     kv("endToEndId", command.endToEndId()),
                     kv("eventId", command.eventId()),
                     kv("eventType", command.eventType()),
                     kv("occurredAt", command.occurredAt()));
            
            transferValidator.validateWebhookEvent(
                command.endToEndId(), 
                command.eventId(), 
                command.eventType(), 
                command.occurredAt()
            );
            
            log.debug("Webhook validation passed", 
                      kv("eventType", command.eventType()));
            
            if (webhookInboxRepositoryPort.existsByEventId(command.eventId())) {
                log.info("Webhook already processed (idempotency check)", 
                         kv("eventId", command.eventId()),
                         kv("reason", "duplicate_event"));
                
                // Record duplicate metric
                metricsService.recordWebhookDuplicated();
                return;
            }
            
            TransferRepositoryPort.TransferResult transfer = transferRepositoryPort.findByEndToEndId(command.endToEndId())
                .orElseThrow(() -> {
                    log.error("Transfer not found for webhook", 
                              kv("endToEndId", command.endToEndId()),
                              kv("eventId", command.eventId()),
                              kv("errorType", "transfer_not_found"));
                    return new IllegalArgumentException("Transfer not found: " + command.endToEndId());
                });
            
            ObservabilityContext.setWalletId(UUID.fromString(transfer.fromWalletId()));
            
            log.info("Transfer found for webhook", 
                     kv("transferId", transfer.id()),
                     kv("currentStatus", transfer.status()),
                     kv("fromWallet", transfer.fromWalletId()),
                     kv("toWallet", transfer.toWalletId()),
                     kv("amount", transfer.amount()),
                     kv("version", transfer.version()));
            
            String newStatus = processWebhookEvent(command.eventType(), transfer);
            
            try {
                transferRepositoryPort.updateStatus(command.endToEndId(), newStatus, transfer.version());
                log.info("Transfer status updated", 
                         kv("endToEndId", command.endToEndId()),
                         kv("oldStatus", transfer.status()),
                         kv("newStatus", newStatus),
                         kv("version", transfer.version()));
            } catch (InvalidTransferStatusTransitionException e) {
                log.warn("Invalid status transition", 
                         kv("endToEndId", command.endToEndId()),
                         kv("currentStatus", transfer.status()),
                         kv("attemptedStatus", newStatus),
                         kv("errorType", "invalid_transition"));
                throw e; // propagate for handler (409)
            } catch (IllegalStateException e) {
                // Version mismatch or other state error
                log.error("State error updating transfer status", 
                          kv("endToEndId", command.endToEndId()),
                          kv("expectedVersion", transfer.version()),
                          kv("errorType", "optimistic_lock_failure"),
                          kv("errorMessage", e.getMessage()));
                throw e;
            } catch (Exception e) {
                log.error("Unexpected failure updating transfer status", 
                          kv("endToEndId", command.endToEndId()),
                          kv("errorType", "unexpected_error"),
                          kv("errorMessage", e.getMessage()));
                throw new RuntimeException("Failed to update transfer status", e);
            }
            
            var webhookEvent = new WebhookInboxRepositoryPort.WebhookEvent(
                UUID.randomUUID(),
                command.endToEndId(),
                command.eventId(),
                command.eventType(),
                command.occurredAt(),
                Instant.now()
            );
            
            webhookInboxRepositoryPort.save(webhookEvent);
            
            metricsService.recordWebhookProcessing(metricsTimer);
            
            log.info("PIX webhook processed successfully", 
                     kv("eventId", command.eventId()),
                     kv("endToEndId", command.endToEndId()),
                     kv("finalStatus", newStatus),
                     kv("eventType", command.eventType()));
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            String errorType = determineWebhookErrorType(e);
            metricsService.recordWebhookProcessingError(metricsTimer, errorType);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing webhook", 
                      kv("eventId", command.eventId()),
                      kv("endToEndId", command.endToEndId()),
                      kv("errorType", "unexpected_error"),
                      kv("errorMessage", e.getMessage()),
                      e);
            metricsService.recordWebhookProcessingError(metricsTimer, "unexpected_error");
            throw new RuntimeException("Failed to process PIX webhook", e);
        } finally {
            ObservabilityContext.clear();
        }
    }
    
    private String determineWebhookErrorType(Exception e) {
        String message = e.getMessage();
        if (message == null) return "unknown";
        
        if (message.contains("Transfer not found")) return "transfer_not_found";
        if (message.contains("required")) return "validation_error";
        if (message.contains("modified by another process")) return "concurrent_modification";
        
        return "business_error";
    }
    
    private String processWebhookEvent(String eventType, TransferRepositoryPort.TransferResult transfer) {
        return switch (eventType.toUpperCase()) {
            case "CONFIRMED" -> {
                log.info("Processing CONFIRMED event - applying transfer to wallets", 
                         kv("eventType", "CONFIRMED"),
                         kv("transferId", transfer.id()));
                applyTransferToWallets(transfer);
                
                // Calculate end-to-end duration (from transfer creation to confirmation)
                java.time.Duration endToEndDuration = java.time.Duration.between(
                    transfer.createdAt(), 
                    java.time.Instant.now()
                );
                
                metricsService.recordTransferConfirmed(endToEndDuration);
                
                log.debug("Transfer end-to-end time recorded",
                         kv("transferId", transfer.id()),
                         kv("endToEndDurationMs", endToEndDuration.toMillis()));
                
                yield "CONFIRMED";
            }
            case "REJECTED" -> {
                log.info("Processing REJECTED event - transfer will not be applied", 
                         kv("eventType", "REJECTED"),
                         kv("transferId", transfer.id()),
                         kv("reason", "transfer_rejected"));
                
                // Record transfer rejected metric
                metricsService.recordTransferRejected();
                
                // If transfer was already applied (shouldn't happen), we'd need to reverse it
                // For now, we just mark as rejected
                yield "REJECTED";
            }
            case "PENDING" -> {
                log.info("Processing PENDING event - keeping status as pending", 
                         kv("eventType", "PENDING"),
                         kv("transferId", transfer.id()));
                yield "PENDING";
            }
            default -> {
                log.warn("Unknown event type - keeping current status", 
                         kv("eventType", eventType),
                         kv("currentStatus", transfer.status()),
                         kv("transferId", transfer.id()));
                yield transfer.status();
            }
        };
    }
    
    private void applyTransferToWallets(TransferRepositoryPort.TransferResult transfer) {
        // Only apply if not already confirmed
        if ("CONFIRMED".equals(transfer.status())) {
            log.info("Transfer already confirmed - skipping wallet operations", 
                     kv("transferId", transfer.id()),
                     kv("status", transfer.status()),
                     kv("reason", "already_confirmed"));
            return;
        }
        
        String idempotencyKey = transfer.endToEndId() + "-apply";
        
        // Check if already applied using ledger idempotency
        if (ledgerEntryRepositoryPort.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Transfer already applied to wallets - skipping (idempotent)", 
                     kv("idempotencyKey", idempotencyKey),
                     kv("transferId", transfer.id()),
                     kv("reason", "already_applied"));
            return;
        }
        
        log.debug("Applying transfer to wallets", 
                  kv("fromWallet", transfer.fromWalletId()),
                  kv("toWallet", transfer.toWalletId()),
                  kv("amount", transfer.amount()));
        
        // Debit from source wallet
        ledgerEntryRepositoryPort.withdraw(
            transfer.fromWalletId(), 
            transfer.amount(), 
            idempotencyKey + "-debit"
        );
        
        log.info("Debited from source wallet", 
                 kv("walletId", transfer.fromWalletId()),
                 kv("amount", transfer.amount()),
                 kv("operation", "DEBIT"));
        
        // Credit to destination wallet
        ledgerEntryRepositoryPort.deposit(
            transfer.toWalletId(),
            transfer.amount(),
            idempotencyKey + "-credit"
        );
        
        log.info("Credited to destination wallet", 
                 kv("walletId", transfer.toWalletId()),
                 kv("amount", transfer.amount()),
                 kv("operation", "CREDIT"));
        
        log.info("Transfer successfully applied to wallets", 
                 kv("transferId", transfer.id()),
                 kv("fromWallet", transfer.fromWalletId()),
                 kv("toWallet", transfer.toWalletId()),
                 kv("amount", transfer.amount()));
    }
}

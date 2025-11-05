package org.pix.wallet.domain.validator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain validator for PIX transfer validation rules.
 * Centralizes transfer-related business validations.
 */
@Component
public class TransferValidator {

    /**
     * Validates that the transfer amount is valid (positive and within limits).
     * 
     * @param amount The transfer amount
     * @throws IllegalArgumentException if amount is invalid
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Transfer amount is required");
        }
        
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        
        // Business rule: maximum transfer amount
        BigDecimal maxAmount = new BigDecimal("100000.00");
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Transfer amount exceeds maximum limit of R$ 100,000.00");
        }
    }
    
    /**
     * Validates that source and destination wallets are different.
     * 
     * @param fromWalletId Source wallet ID
     * @param toWalletId Destination wallet ID
     * @throws IllegalArgumentException if wallets are the same
     */
    public void validateDifferentWallets(UUID fromWalletId, UUID toWalletId) {
        if (fromWalletId == null) {
            throw new IllegalArgumentException("Source wallet ID is required");
        }
        
        if (toWalletId == null) {
            throw new IllegalArgumentException("Destination wallet ID is required");
        }
        
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
    }
    
    /**
     * Validates webhook event data.
     * 
     * @param endToEndId End-to-end ID
     * @param eventId Event ID
     * @param eventType Event type
     * @param occurredAt Timestamp when event occurred
     * @throws IllegalArgumentException if any field is invalid
     */
    public void validateWebhookEvent(String endToEndId, String eventId, String eventType, Instant occurredAt) {
        if (endToEndId == null || endToEndId.isBlank()) {
            throw new IllegalArgumentException("End to end ID is required");
        }
        
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at timestamp is required");
        }
        
        // Business rule: event cannot be in the future
        if (occurredAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Event timestamp cannot be in the future");
        }
    }
    
    /**
     * Validates that event type is supported.
     * 
     * @param eventType The event type
     * @return The normalized event type (uppercase)
     * @throws IllegalArgumentException if event type is not supported
     */
    public String validateAndNormalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        String normalized = eventType.toUpperCase();
        
        // Business rule: only these event types are supported
        if (!normalized.matches("CONFIRMED|REJECTED|PENDING")) {
            throw new IllegalArgumentException("Unsupported event type: " + eventType + 
                ". Supported types: CONFIRMED, REJECTED, PENDING");
        }
        
        return normalized;
    }
}

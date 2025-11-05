package org.pix.wallet.domain.validator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.pix.wallet.domain.validator.ValidationConstants.Messages.*;
import static org.pix.wallet.domain.validator.ValidationConstants.Transfer.*;

/**
 * Domain validator for PIX transfer validation rules.
 * 
 * <p>Centralizes transfer-related business validations including:</p>
 * 
 * <ul>
 *   <li><b>Amount validation:</b> Must be positive and ≤ R$ 100,000.00</li>
 *   <li><b>Wallet validation:</b> Source and destination must be different</li>
 *   <li><b>Webhook validation:</b> Required fields and timestamp checks</li>
 *   <li><b>Event type validation:</b> Only CONFIRMED, REJECTED, PENDING supported</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * TransferValidator validator = new TransferValidator();
 * validator.validateAmount(new BigDecimal("500.00"));
 * validator.validateDifferentWallets(sourceId, destId);
 * validator.validateWebhookEvent(endToEndId, eventId, eventType, timestamp);
 * </pre>
 * 
 * @author PIX Wallet Team
 * @since 1.0
 */
@Component
public class TransferValidator {

    /**
     * Validates that the transfer amount is valid (positive and within limits).
     * 
     * <p><b>Business Rules:</b></p>
     * <ul>
     *   <li>Amount is required (not null)</li>
     *   <li>Amount must be greater than zero</li>
     *   <li>Amount cannot exceed R$ 100,000.00</li>
     * </ul>
     * 
     * @param amount The transfer amount
     * @throws IllegalArgumentException if amount is null, zero, negative, or exceeds limit
     */
    public void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException(AMOUNT_REQUIRED);
        }
        
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
        }
        
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(AMOUNT_EXCEEDS_LIMIT);
        }
    }
    
    /**
     * Validates that source and destination wallets are different.
     * 
     * <p>Prevents self-transfers which could cause accounting inconsistencies.</p>
     * 
     * @param fromWalletId Source wallet ID
     * @param toWalletId Destination wallet ID
     * @throws IllegalArgumentException if either wallet is null or both are the same
     */
    public void validateDifferentWallets(UUID fromWalletId, UUID toWalletId) {
        if (fromWalletId == null) {
            throw new IllegalArgumentException(SOURCE_WALLET_REQUIRED);
        }
        
        if (toWalletId == null) {
            throw new IllegalArgumentException(DESTINATION_WALLET_REQUIRED);
        }
        
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException(SAME_WALLET_TRANSFER);
        }
    }
    
    /**
     * Validates webhook event data.
     * 
     * <p><b>Validation Rules:</b></p>
     * <ul>
     *   <li>All fields are required (not null or blank)</li>
     *   <li>Timestamp cannot be in the future (prevents fraudulent backdating)</li>
     * </ul>
     * 
     * @param endToEndId End-to-end ID (unique transaction identifier from Brazilian Central Bank)
     * @param eventId Event ID (unique event identifier for idempotency)
     * @param eventType Event type (CONFIRMED, REJECTED, or PENDING)
     * @param occurredAt Timestamp when event occurred
     * @throws IllegalArgumentException if any field is invalid or timestamp is in future
     */
    public void validateWebhookEvent(String endToEndId, String eventId, String eventType, Instant occurredAt) {
        if (endToEndId == null || endToEndId.isBlank()) {
            throw new IllegalArgumentException(END_TO_END_ID_REQUIRED);
        }
        
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(EVENT_ID_REQUIRED);
        }
        
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(EVENT_TYPE_REQUIRED);
        }
        
        if (occurredAt == null) {
            throw new IllegalArgumentException(OCCURRED_AT_REQUIRED);
        }
        
        // Business rule: event cannot be in the future
        if (occurredAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(EVENT_IN_FUTURE);
        }
    }
    
    /**
     * Validates and normalizes event type to uppercase.
     * 
     * <p><b>Supported Event Types:</b></p>
     * <ul>
     *   <li><b>CONFIRMED:</b> Transfer approved, funds will be debited/credited</li>
     *   <li><b>REJECTED:</b> Transfer denied, no wallet changes</li>
     *   <li><b>PENDING:</b> Transfer awaiting approval</li>
     * </ul>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * String normalized = validateAndNormalizeEventType("confirmed");
     * // Returns: "CONFIRMED"
     * </pre>
     * 
     * @param eventType The event type (case-insensitive)
     * @return The normalized event type (uppercase)
     * @throws IllegalArgumentException if event type is null, blank, or not supported
     */
    public String validateAndNormalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(EVENT_TYPE_REQUIRED);
        }
        
        String normalized = eventType.toUpperCase();
        
        if (!normalized.matches(SUPPORTED_EVENT_TYPES_PATTERN)) {
            throw new IllegalArgumentException(String.format(EVENT_TYPE_UNSUPPORTED, eventType));
        }
        
        return normalized;
    }
}

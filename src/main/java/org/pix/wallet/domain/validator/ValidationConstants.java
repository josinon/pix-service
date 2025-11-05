package org.pix.wallet.domain.validator;

import java.math.BigDecimal;

/**
 * Central repository for validation-related constants.
 * Consolidates all magic numbers and business rules in one place.
 * 
 * @since 1.0
 */
public final class ValidationConstants {

    private ValidationConstants() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * PIX Key validation constants
     */
    public static final class PixKey {
        private PixKey() {}
        
        /** CPF must have exactly 11 digits */
        public static final String CPF_PATTERN = "\\d{11}";
        public static final int CPF_LENGTH = 11;
        
        /** Email basic pattern and maximum length */
        public static final String EMAIL_PATTERN = ".+@.+\\..+";
        public static final int EMAIL_MAX_LENGTH = 120;
        
        /** Phone must be in international format: +[11-14 digits] */
        public static final String PHONE_PATTERN = "\\+\\d{11,14}";
        public static final int PHONE_MIN_LENGTH = 12; // + plus 11 digits
        public static final int PHONE_MAX_LENGTH = 15; // + plus 14 digits
        
        /** Random PIX key must be 32 hexadecimal characters (UUID without hyphens) */
        public static final String RANDOM_PATTERN = "[a-f0-9]{32}";
        public static final int RANDOM_LENGTH = 32;
    }

    /**
     * Transfer validation constants
     */
    public static final class Transfer {
        private Transfer() {}
        
        /** Maximum allowed transfer amount: R$ 100,000.00 */
        public static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");
        
        /** Supported webhook event types */
        public static final String SUPPORTED_EVENT_TYPES_PATTERN = "CONFIRMED|REJECTED|PENDING";
    }

    /**
     * Validation error messages
     */
    public static final class Messages {
        private Messages() {}
        
        // PIX Key messages
        public static final String PIX_KEY_TYPE_REQUIRED = "PIX key type is required";
        public static final String PIX_KEY_VALUE_REQUIRED = "PIX key value is required";
        public static final String PIX_KEY_TYPE_UNSUPPORTED = "Unsupported PIX key type: %s";
        
        public static final String CPF_INVALID_FORMAT = "Invalid CPF format. Expected 11 digits, got: %s";
        public static final String EMAIL_INVALID_FORMAT = "Invalid email format: %s";
        public static final String EMAIL_TOO_LONG = "Email too long. Maximum 120 characters";
        public static final String PHONE_INVALID_FORMAT = "Invalid phone format. Expected: +[11-14 digits], got: %s";
        public static final String RANDOM_KEY_INVALID_FORMAT = "Invalid random PIX key format. Expected 32 hexadecimal characters";
        
        // Transfer messages
        public static final String AMOUNT_REQUIRED = "Transfer amount is required";
        public static final String AMOUNT_MUST_BE_POSITIVE = "Transfer amount must be greater than zero";
        public static final String AMOUNT_EXCEEDS_LIMIT = "Transfer amount exceeds maximum limit of R$ 100,000.00";
        
        public static final String SOURCE_WALLET_REQUIRED = "Source wallet ID is required";
        public static final String DESTINATION_WALLET_REQUIRED = "Destination wallet ID is required";
        public static final String SAME_WALLET_TRANSFER = "Cannot transfer to the same wallet";
        
        // Webhook messages
        public static final String END_TO_END_ID_REQUIRED = "End to end ID is required";
        public static final String EVENT_ID_REQUIRED = "Event ID is required";
        public static final String EVENT_TYPE_REQUIRED = "Event type is required";
        public static final String OCCURRED_AT_REQUIRED = "Occurred at timestamp is required";
        public static final String EVENT_IN_FUTURE = "Event timestamp cannot be in the future";
        public static final String EVENT_TYPE_UNSUPPORTED = "Unsupported event type: %s. Supported types: CONFIRMED, REJECTED, PENDING";
    }
}

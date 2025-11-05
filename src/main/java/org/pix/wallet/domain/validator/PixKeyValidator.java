package org.pix.wallet.domain.validator;

import org.pix.wallet.domain.model.enums.PixKeyType;
import org.springframework.stereotype.Component;

import static org.pix.wallet.domain.validator.ValidationConstants.Messages.*;
import static org.pix.wallet.domain.validator.ValidationConstants.PixKey.*;

/**
 * Domain validator for PIX key validation rules.
 * 
 * <p>Centralizes all PIX key format validation logic according to Brazilian
 * Central Bank PIX specifications. Validates the following key types:</p>
 * 
 * <ul>
 *   <li><b>CPF:</b> Exactly 11 digits (e.g., "12345678901")</li>
 *   <li><b>EMAIL:</b> Valid email format, max 120 characters (e.g., "user@example.com")</li>
 *   <li><b>PHONE:</b> International format +[11-14 digits] (e.g., "+5511999999999")</li>
 *   <li><b>RANDOM:</b> 32 hexadecimal characters, UUID without hyphens (e.g., "a1b2c3d4...")</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * PixKeyValidator validator = new PixKeyValidator();
 * validator.validate(PixKeyType.CPF, "12345678901");
 * String randomKey = validator.normalizeAndGenerate(PixKeyType.RANDOM, null);
 * </pre>
 * 
 * @author PIX Wallet Team
 * @since 1.0
 * @see PixKeyType
 */
@Component
public class PixKeyValidator {

    /**
     * Validates PIX key format based on its type.
     * 
     * @param type The PIX key type (CPF, EMAIL, PHONE, RANDOM)
     * @param value The PIX key value to validate
     * @throws IllegalArgumentException if validation fails with descriptive error message
     */
    public void validate(PixKeyType type, String value) {
        if (type == null) {
            throw new IllegalArgumentException(PIX_KEY_TYPE_REQUIRED);
        }
        
        if (value == null) {
            throw new IllegalArgumentException(PIX_KEY_VALUE_REQUIRED);
        }
        
        switch (type) {
            case CPF -> validateCPF(value);
            case EMAIL -> validateEmail(value);
            case PHONE -> validatePhone(value);
            case RANDOM -> validateRandom(value);
            default -> throw new IllegalArgumentException(String.format(PIX_KEY_TYPE_UNSUPPORTED, type));
        }
    }
    
    /**
     * Validates CPF format (11 digits).
     * 
     * @param cpf The CPF value to validate
     * @throws IllegalArgumentException if CPF doesn't match expected format
     */
    private void validateCPF(String cpf) {
        if (!cpf.matches(CPF_PATTERN)) {
            throw new IllegalArgumentException(String.format(CPF_INVALID_FORMAT, cpf));
        }
    }
    
    /**
     * Validates email format (basic pattern + length check).
     * 
     * @param email The email value to validate
     * @throws IllegalArgumentException if email format is invalid or too long
     */
    private void validateEmail(String email) {
        if (!email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException(String.format(EMAIL_INVALID_FORMAT, email));
        }
        
        if (email.length() > EMAIL_MAX_LENGTH) {
            throw new IllegalArgumentException(EMAIL_TOO_LONG);
        }
    }
    
    /**
     * Validates phone format (international format with +).
     * 
     * @param phone The phone value to validate
     * @throws IllegalArgumentException if phone doesn't match expected format
     */
    private void validatePhone(String phone) {
        if (!phone.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(String.format(PHONE_INVALID_FORMAT, phone));
        }
    }
    
    /**
     * Validates random PIX key (32 hexadecimal characters without hyphens).
     * 
     * @param random The random key value to validate
     * @throws IllegalArgumentException if random key format is invalid
     */
    private void validateRandom(String random) {
        if (!random.matches(RANDOM_PATTERN)) {
            throw new IllegalArgumentException(RANDOM_KEY_INVALID_FORMAT);
        }
    }
    
    /**
     * Normalizes and generates PIX key value based on type.
     * 
     * <p>For RANDOM type, generates a new UUID-based key (32 hex chars).
     * For other types, trims whitespace and returns the value.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * // Generates new random key
     * String key = normalizeAndGenerate(PixKeyType.RANDOM, null);
     * // Returns: "a1b2c3d4e5f6789..." (32 chars)
     * 
     * // Normalizes existing value
     * String email = normalizeAndGenerate(PixKeyType.EMAIL, "  user@example.com  ");
     * // Returns: "user@example.com"
     * </pre>
     * 
     * @param type The PIX key type
     * @param value The input value (can be null for RANDOM)
     * @return The normalized or generated PIX key value
     */
    public String normalizeAndGenerate(PixKeyType type, String value) {
        if (type == PixKeyType.RANDOM) {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
        
        return value == null ? "" : value.trim();
    }
}

package org.pix.wallet.domain.validator;

import org.pix.wallet.domain.model.enums.PixKeyType;
import org.springframework.stereotype.Component;

/**
 * Domain validator for PIX key validation rules.
 * Centralizes all PIX key format validation logic.
 */
@Component
public class PixKeyValidator {

    /**
     * Validates PIX key format based on its type.
     * 
     * @param type The PIX key type (CPF, EMAIL, PHONE, RANDOM)
     * @param value The PIX key value to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(PixKeyType type, String value) {
        if (type == null) {
            throw new IllegalArgumentException("PIX key type is required");
        }
        
        if (value == null) {
            throw new IllegalArgumentException("PIX key value is required");
        }
        
        switch (type) {
            case CPF -> validateCPF(value);
            case EMAIL -> validateEmail(value);
            case PHONE -> validatePhone(value);
            case RANDOM -> validateRandom(value);
            default -> throw new IllegalArgumentException("Unsupported PIX key type: " + type);
        }
    }
    
    /**
     * Validates CPF format (11 digits).
     */
    private void validateCPF(String cpf) {
        if (!cpf.matches("\\d{11}")) {
            throw new IllegalArgumentException("Invalid CPF format. Expected 11 digits, got: " + cpf);
        }
    }
    
    /**
     * Validates email format (basic pattern).
     */
    private void validateEmail(String email) {
        if (!email.matches(".+@.+\\..+")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        
        if (email.length() > 120) {
            throw new IllegalArgumentException("Email too long. Maximum 120 characters");
        }
    }
    
    /**
     * Validates phone format (international format with +).
     */
    private void validatePhone(String phone) {
        if (!phone.matches("\\+\\d{11,14}")) {
            throw new IllegalArgumentException("Invalid phone format. Expected: +[11-14 digits], got: " + phone);
        }
    }
    
    /**
     * Validates random PIX key (32 alphanumeric characters without hyphens).
     */
    private void validateRandom(String random) {
        if (!random.matches("[a-f0-9]{32}")) {
            throw new IllegalArgumentException("Invalid random PIX key format. Expected 32 hexadecimal characters");
        }
    }
    
    /**
     * Normalizes and generates PIX key value based on type.
     * For RANDOM type, generates a new UUID-based key.
     * For other types, trims and returns the value.
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
